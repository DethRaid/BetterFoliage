package mods.betterfoliage.client.texture;

import java.util.Map;

import mods.betterfoliage.BetterFoliage;
import mods.betterfoliage.client.texture.BaseTextures.BaseInfo;
import mods.betterfoliage.client.texture.models.IModelTextureMapping;
import mods.betterfoliage.common.config.Config;
import net.minecraft.block.state.IBlockState;
import net.minecraft.client.resources.model.ModelResourceLocation;
import net.minecraftforge.client.model.IModel;

public abstract class SimpleTextures extends BaseTextures {
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
	 * Allows subclasses to check if their thing matches blocks. I'm not sure how this works but I know how to make it work.
	 * 
	 * @param stateMapping The mapping to check or something
	 * 
	 * @return Whether or not the block matches
	 */
	protected abstract boolean checkBlockMatching(Map.Entry<IBlockState, ModelResourceLocation> stateMapping);
    
    /**
     * Factory method that allows child classes to provide their own Info classes
     * 
     * @param resolvedName The name of the info
     * @return A new BaseInfo (or subclass thereof)
     */
    protected abstract BaseInfo infoFactory(String resolvedName);
}
