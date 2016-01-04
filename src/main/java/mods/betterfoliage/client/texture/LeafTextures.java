package mods.betterfoliage.client.texture;

import java.util.Collection;
import java.util.Map;

import mods.betterfoliage.BetterFoliage;
import mods.betterfoliage.client.event.PostLoadModelDefinitionsEvent;
import mods.betterfoliage.client.misc.TextureMatcher;
import mods.betterfoliage.client.render.TextureSet;
import mods.betterfoliage.client.render.impl.EntityFXFallingLeaves;
import mods.betterfoliage.client.render.impl.primitives.Color4;
import mods.betterfoliage.client.texture.generator.LeafGenerator;
import mods.betterfoliage.client.texture.models.IModelTextureMapping;
import mods.betterfoliage.client.util.BFFunctions;
import mods.betterfoliage.client.util.ResourceUtils;
import mods.betterfoliage.common.config.Config;
import mods.betterfoliage.loader.impl.CodeRefs;
import net.minecraft.block.Block;
import net.minecraft.block.state.IBlockState;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.renderer.texture.TextureMap;
import net.minecraft.client.resources.model.ModelResourceLocation;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.client.event.TextureStitchEvent;
import net.minecraftforge.client.model.IModel;
import net.minecraftforge.client.model.ModelLoader;
import net.minecraftforge.fml.common.eventhandler.EventPriority;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;

/** Handles leaf block discovery and classification
 * @author octarine-noise
 */
@SideOnly(Side.CLIENT)
public class LeafTextures extends SimpleTextures {

    /** Rendering information for a leaf block 
     * @author octarine-noise
     */
    public static class LeafInfo extends BaseInfo {
		public TextureAtlasSprite roundLeafTexture;
        public String particleType;
        
        public LeafInfo(String baseTextureName) {
			super(baseTextureName);
		}
    }
    
    /** Leaf block rendering information */
    public Map<IBlockState, LeafInfo> leafInfoMap = Maps.newHashMap();
    
    /** Textures for leaf particles */
    public Map<String, TextureSet> particleTextures = Maps.newHashMap();
    
    /** Leaf type mappings */
    public TextureMatcher particleTypes = new TextureMatcher();
    
    public int loadedParticles;
    
    @SubscribeEvent(priority=EventPriority.HIGH)
    public void handleTextureReload(TextureStitchEvent.Pre event) {
        if (event.map != blockTextures) return;
        
        for (Map.Entry<IBlockState, LeafInfo> entry : leafInfoMap.entrySet()) {
            ResourceLocation baseTextureLocation = new ResourceLocation(entry.getValue().baseTextureName);
            ResourceLocation roundTextureLocation = new ResourceLocation(LeafGenerator.DOMAIN, baseTextureLocation.toString());
            TextureAtlasSprite baseLeafTexture = blockTextures.getTextureExtry(baseTextureLocation.toString());
            TextureAtlasSprite roundLeafTexture = blockTextures.getTextureExtry(roundTextureLocation.toString());
            if (roundLeafTexture == null) roundLeafTexture = blockTextures.registerSprite(roundTextureLocation);
            
            // register leaf particles
            entry.getValue().particleType = particleTypes.get(baseLeafTexture);
            if (!particleTextures.containsKey(entry.getValue().particleType)) {
                TextureSet newSet = new TextureSet(BetterFoliage.DOMAIN, String.format("blocks/falling_leaf_%s_%%d", entry.getValue().particleType));
                newSet.registerSprites(blockTextures);
                particleTextures.put(entry.getValue().particleType, newSet);
                loadedParticles++;
            }
            
            entry.getValue().roundLeafTexture = roundLeafTexture;
            entry.getValue().averageColor = ResourceUtils.calculateTextureColor(baseLeafTexture);
            entry.getValue().particleType = particleTypes.get(baseLeafTexture);
        }
    }
        
    @SubscribeEvent
    public void endTextureReload(TextureStitchEvent.Post event) {
        if (event.map != blockTextures) return;
        blockTextures = null;
        BetterFoliage.log.info(String.format("Loaded %d leaf particle sets", loadedParticles));
    }

    @Override
    protected LeafInfo infoFactory(String resolvedName) {
    	return new LeafInfo(resolvedName);
    }
    
    @Override
	protected boolean checkBlockMatching(Map.Entry<IBlockState, ModelResourceLocation> stateMapping) {
		return !Config.leaves.matchesClass(stateMapping.getKey().getBlock());
	}
}
