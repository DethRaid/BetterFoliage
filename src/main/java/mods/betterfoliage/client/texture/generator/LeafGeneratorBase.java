package mods.betterfoliage.client.texture.generator;

import java.awt.image.BufferedImage;
import java.io.IOException;
import java.util.Map;

import javax.imageio.ImageIO;

import mods.betterfoliage.BetterFoliage;
import mods.betterfoliage.client.texture.LeafTextureEnumerator.LeafTextureFoundEvent;
import mods.betterfoliage.client.util.ResourceUtils;
import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.IResource;
import net.minecraft.client.resources.IResourceManager;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.client.event.TextureStitchEvent.Pre;

import com.google.common.collect.Maps;

import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;

/** Texture generator base class for textures based on leaf blocks.
 * Supports loading from resource packs instead of generating if available.
 * @author octarine-noise
 */
@SideOnly(Side.CLIENT)
public abstract class LeafGeneratorBase extends BlockTextureGenerator {

	@SideOnly(Side.CLIENT)
	public static class TextureGenerationException extends Exception {
		private static final long serialVersionUID = 7339757761980002651L;
	};
	
	/** Format string of pre-drawn texture location */
	public String customLocationFormat;
	
	/** Format string of alpha mask location */
	public String maskImageLocationFormat;
	
	/** Resource domain name of pre-drawn textures */
	public String nonGeneratedDomain;
	
	/** Number of textures generated in the current run */
	public int generatedCounter = 0;
	
	/** Number of pre-drawn textures found in the current run */
	public int drawnCounter = 0;
	
	public Map<ResourceLocation, IResource> resourceCache = Maps.newHashMap(); 
	
	public LeafGeneratorBase(String domain, String nonGeneratedDomain, String handDrawnLocationFormat, String maskImageLocationFormat, ResourceLocation missingResource) {
		super(domain, missingResource);
		this.nonGeneratedDomain = nonGeneratedDomain;
		this.customLocationFormat = handDrawnLocationFormat;
		this.maskImageLocationFormat = maskImageLocationFormat;
	}

	@Override
	public IResource getResource(ResourceLocation resourceLocation) throws IOException {
		IResource cached = resourceCache.get(resourceLocation);
		if (cached != null) return cached;
		
		IResourceManager resourceManager = Minecraft.getMinecraft().getResourceManager();
		ResourceLocation originalResource = ResourceUtils.unwrapResource(resourceLocation);
		
		// check for provided texture
		ResourceLocation customLocation = getCustomLocation(originalResource);
		if (ResourceUtils.resourceExists(customLocation)) {
			drawnCounter++;
			return resourceManager.getResource(customLocation);
		}
		
		// use animation metadata as-is
		if (resourceLocation.getResourcePath().toLowerCase().endsWith(".mcmeta")) return resourceManager.getResource(originalResource);
		
		// generate our own
		if (!ResourceUtils.resourceExists(originalResource)) {
			BetterFoliage.log.info(String.format("Could not find resource: %s", originalResource.toString()));
			return getMissingResource();
		}
		
		BufferedImage result;
		try {
			result = generateLeaf(originalResource);
		} catch (TextureGenerationException e) {
			BetterFoliage.log.info(String.format("Error generating leaf for resource: %s", originalResource.toString()));
			return getMissingResource();
		}
		generatedCounter++;
		IResource generated = new BufferedImageResource(result, originalResource);
		resourceCache.put(resourceLocation, generated);
		return generated;
		
	}
	
	protected abstract BufferedImage generateLeaf(ResourceLocation originalWithDirs) throws IOException, TextureGenerationException;
	
	/** Loads the alpha mask of the given type and size. If a mask of the exact size can not be found,
	 *  will try to load progressively smaller masks down to 16x16
	 * @param type mask type
	 * @param size texture size
	 * @return alpha mask
	 */
	protected BufferedImage loadLeafMaskImage(String type, int size) {
		IResourceManager resourceManager = Minecraft.getMinecraft().getResourceManager();
		IResource maskResource = null;
		
		while (maskResource == null && size >= 1) {
			try {
				maskResource = resourceManager.getResource(new ResourceLocation(String.format(maskImageLocationFormat, size, type)));
			} catch (Exception e) {}
			size /= 2;
		}
		
		try {
			return maskResource == null ? null : ImageIO.read(maskResource.getInputStream());
		} catch (IOException e) {
			return null;
		}
	}
	
	@Override
	@SubscribeEvent
	public void handleTextureReload(Pre event) {
		super.handleTextureReload(event);
		if (event.map.getTextureType() != 0) return;
		generatedCounter = 0;
		drawnCounter = 0;
		resourceCache.clear();
	}

	@SubscribeEvent
	public void handleRegisterTexture(LeafTextureFoundEvent event) {
		event.blockTextures.registerIcon(getRoundLocationShort(event.icon.getIconName()).toString());
	}

	public ResourceLocation getCustomLocation(ResourceLocation baseLocation) {
		return new ResourceLocation(nonGeneratedDomain, String.format(customLocationFormat, baseLocation.getResourceDomain(), baseLocation.getResourcePath())); 
	}
	
	public ResourceLocation getRoundLocationShort(String iconName) {
		return new ResourceLocation(domainName, iconName);
	}
	
	
	public ResourceLocation getRoundLocationFull(String iconName) {
		return new ResourceLocation(domainName, String.format("textures/blocks/%s.png", new ResourceLocation(iconName).toString()));
	}
	
}
