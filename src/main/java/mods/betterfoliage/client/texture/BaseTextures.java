package mods.betterfoliage.client.texture;

import java.io.IOException;
import java.lang.reflect.Field;
import java.util.Collection;
import java.util.Map;

import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import mods.betterfoliage.BetterFoliage;
import mods.betterfoliage.client.event.PostLoadModelDefinitionsEvent;
import mods.betterfoliage.client.render.impl.primitives.Color4;
import mods.betterfoliage.client.texture.models.IModelTextureMapping;
import mods.betterfoliage.client.util.BFFunctions;
import mods.betterfoliage.client.util.ResourceUtils;
import mods.betterfoliage.common.config.Config;
import mods.betterfoliage.loader.impl.CodeRefs;
import net.minecraft.block.Block;
import net.minecraft.block.state.IBlockState;
import net.minecraft.client.renderer.BlockModelShapes;
import net.minecraft.client.renderer.block.statemap.BlockStateMapper;
import net.minecraft.client.renderer.block.statemap.IStateMapper;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.renderer.texture.TextureMap;
import net.minecraft.client.resources.model.ModelBakery;
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

@SideOnly(Side.CLIENT)
public abstract class BaseTextures {
	private static final Logger LOG = LogManager.getLogger(BaseTextures.class);
	
    /** Rendering information for a grass block 
     * @author octarine-noise
     */
    public static class BaseInfo {
        public String baseTextureName;
        public Color4 averageColor;
        
        public BaseInfo(String baseTextureName) {
            this.baseTextureName = baseTextureName;
        }
    }
    
    /** {@link TextureMap} used in {@link ModelLoader} in the current run */
    public TextureMap blockTextures;
    
    public Collection<IModelTextureMapping> mappings = Lists.newLinkedList();
    
    /** Grass block rendering information */
    public Map<IBlockState, BaseInfo> textureInfoMap = Maps.newHashMap();

    /**
     * Cache the fields so we don't have to re-examine classes each frame
     */
    protected Field modelBakeryTextureMap;
    protected Field modelBakeryBlockModelShapes;
	protected Field blockStateMapperBlockStateMap;
	
	/**
	 * Loads the fields to access a couple now-private things.
	 */
	public BaseTextures() {
		try {
			modelBakeryTextureMap = ModelBakery.class.getDeclaredField("textureMap");
			modelBakeryBlockModelShapes = ModelBakery.class.getDeclaredField("blockModelShapes");
			blockStateMapperBlockStateMap = BlockStateMapper.class.getDeclaredField("blockStateMap");
		
		} catch (NoSuchFieldException e) {
			LOG.log(Level.FATAL, "Could not find the field I wanted, cannot reasonably continue. Minecraft will crash", e);
		
		} catch (SecurityException e) {
			LOG.log(Level.FATAL, "Security mom is mad. Hiding the closet until the security dad comes home. Cannot reasonably continue. Minecraft will crash", e);
		}
	}
    
    @SuppressWarnings("unchecked")
    @SubscribeEvent
    public void handlePostLoadModelDefinitions(PostLoadModelDefinitionsEvent event) {
    	try {
	        blockTextures = (TextureMap) modelBakeryTextureMap.get(event.loader);
	        textureInfoMap.clear();
	        
	        Map<ModelResourceLocation, IModel> stateModels = CodeRefs.fStateModels.getInstanceField(event.loader);
	        
	        BlockModelShapes blockModelShapes = (BlockModelShapes) modelBakeryBlockModelShapes.get(event.loader);
	        Map<Block, IStateMapper> blockStateMap = (Map<Block, IStateMapper>) blockStateMapperBlockStateMap.get(blockModelShapes.getBlockStateMapper());
	        
	        Iterable<Map.Entry<IBlockState, ModelResourceLocation>> stateMappings =
	        Iterables.concat(
	            Iterables.transform(
	                Iterables.transform(
	                    Block.blockRegistry,
	                    BFFunctions.getBlockStateMappings(blockStateMap)),
	                BFFunctions.<IBlockState, ModelResourceLocation>asEntries()
	            )
	        );
	        
	        loopOverStateMappings(stateModels, stateMappings);
    	
    	} catch(Exception e ) {
    		LOG.log(Level.FATAL, "Could not get needed field data, expect a NPE or something aweful", e);
    	}
    }

	protected void loopOverStateMappings(Map<ModelResourceLocation, IModel> stateModels,
			Iterable<Map.Entry<IBlockState, ModelResourceLocation>> stateMappings) {
		for (Map.Entry<IBlockState, ModelResourceLocation> stateMapping : stateMappings) {
		    if (textureInfoMap.containsKey(stateMapping.getKey())) {
		    	continue;
		    }
		    
		    if (checkBlockMatching(stateMapping)) {
		    	continue;
		    }
		    
		    // this is a blockstate for a grass block, try to find out the base grass top texture
		    IModel model = stateModels.get(stateMapping.getValue());
		    for (IModelTextureMapping mapping : mappings) {
		        String resolvedName = mapping.apply(model);
		        if (resolvedName != null) {
		            // store texture location for this blockstate
		            BetterFoliage.log.debug(String.format("block=%s, texture=%s", stateMapping.getKey().toString(), resolvedName));
		            textureInfoMap.put(stateMapping.getKey(), infoFactory(resolvedName));
		            break;
		        }
		    }
		}
	}

	/**
	 * Allows subclasses to check 
	 * 
	 * @param stateMapping
	 * @return
	 */
	protected boolean checkBlockMatching(Map.Entry<IBlockState, ModelResourceLocation> stateMapping) {
		return !Config.grass.matchesClass(stateMapping.getKey().getBlock());
	}
    
    /**
     * Factory method that allows child classes to provide their own Info classes
     * 
     * @param resolvedName The name of the info
     * @return A new BaseInfo (or subclass thereof)
     */
    protected BaseInfo infoFactory(String resolvedName) {
    	return new BaseInfo(resolvedName);
    }

	protected Color4 getAverageColor(BaseInfo entry) {
		ResourceLocation baseTextureLocation = new ResourceLocation(entry.baseTextureName);
		TextureAtlasSprite baseGrassTexture = blockTextures.getTextureExtry(baseTextureLocation.toString());
		Color4 averageColor = ResourceUtils.calculateTextureColor(baseGrassTexture);
		return averageColor;
	}
    
    @SubscribeEvent
    public void endTextureReload(TextureStitchEvent.Post event) {
        if (event.map != blockTextures) return;
        blockTextures = null;
    }
}
