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
public class GrassTextures extends BaseTextures {
	private static final Logger LOG = LogManager.getLogger(GrassTextures.class);
	
    /** Rendering information for a grass block 
     * @author octarine-noise
     */
    public static class GrassInfo extends BaseInfo {
        public boolean useTextureColor;
        
        public GrassInfo(String baseTextureName) {
            super(baseTextureName);
        }
    }

    /** Grass block rendering information */
    public Map<IBlockState, GrassInfo> grassInfoMap = Maps.newHashMap();

	public GrassTextures() {
		super();
	}
	
    public Color4 getRenderColor(IBlockState blockState, Color4 defaultColor) {
    	GrassInfo info = grassInfoMap.get(blockState);
    	return (info == null || !info.useTextureColor) ? defaultColor : info.averageColor;
    }
    
    @Override
    protected GrassInfo infoFactory(String resolvedName) {
    	return new GrassInfo(resolvedName);
    }
    
    @SubscribeEvent(priority=EventPriority.HIGH)
    public void handleTextureReload(TextureStitchEvent.Pre event) {
        if (event.map != blockTextures) return;
        
        for (Map.Entry<IBlockState, GrassInfo> entry : grassInfoMap.entrySet()) {
            Color4 averageColor = getAverageColor(entry.getValue());
            entry.getValue().averageColor = averageColor.withHSVBrightness(1.0f);
        }
    }
    
    @SubscribeEvent
    public void endTextureReload(TextureStitchEvent.Post event) {
        if (event.map != blockTextures) return;
        blockTextures = null;
    }
}
