package mods.betterfoliage.client.texture.generator;

import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.IOException;

import javax.imageio.ImageIO;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import mods.betterfoliage.client.util.ResourceUtils;
import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.IResource;
import net.minecraft.client.resources.IResourceManager;
import net.minecraft.util.ResourceLocation;

@SideOnly(Side.CLIENT)
public class ReedGenerator extends BlockTextureGenerator {

	public boolean isBottom;
	
	public ReedGenerator(String domainName, ResourceLocation missingResource, boolean isBottom) {
		super(domainName, missingResource);
		this.isBottom = isBottom;
	}

	@Override
	public IResource getResource(ResourceLocation resourceLocation) throws IOException {
		IResourceManager resourceManager = Minecraft.getMinecraft().getResourceManager();
		ResourceLocation originalResource = ResourceUtils.unwrapResource(resourceLocation);
		
		// load full texture
		BufferedImage origImage = ImageIO.read(resourceManager.getResource(originalResource).getInputStream());

		// draw half texture
		BufferedImage result = new BufferedImage(origImage.getWidth(), origImage.getHeight() / 2, BufferedImage.TYPE_4BYTE_ABGR);
		Graphics2D graphics = result.createGraphics();
		graphics.drawImage(origImage, 0, isBottom ? -origImage.getHeight() / 2 : 0, null);
		
		return new BufferedImageResource(result, originalResource);
	}

}
