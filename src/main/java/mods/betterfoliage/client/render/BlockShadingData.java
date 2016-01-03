package mods.betterfoliage.client.render;

import java.lang.reflect.Field;
import java.util.BitSet;

import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import net.minecraft.block.Block;
import net.minecraft.client.renderer.BFAbstractRenderer;
import net.minecraft.client.renderer.BFAbstractRenderer.BFAmbientOcclusionFace;
import net.minecraft.client.renderer.BlockModelRenderer;
import net.minecraft.util.BlockPos;
import net.minecraft.util.EnumFacing;
import net.minecraft.world.IBlockAccess;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

/** Shading data (both AO and simple brightness) for a single world block
 * @author octarine-noise
 */
@SideOnly(Side.CLIENT)
public class BlockShadingData implements IShadingData {
	private static final Logger LOG = LogManager.getLogger(BlockShadingData.class);
	
    /** AO data for all faces */
    public BFAmbientOcclusionFace[] aoFaces = new BFAmbientOcclusionFace[6];
    
    public BitSet shadingBitSet = new BitSet(3);
    
    /** Translucence for all faces */
    public boolean[] isTranslucent = new boolean[6];
    
    /** Mixed block brightness for all faces */
    public int[] mixedBrightness = new int[6];
    
    /** Currently using AO or not */
    public boolean useAO;

    /** Quick lookup: vertex index in {@link BFAmbientOcclusionFace} arrays for the block corner specified by 3 {@link EnumFacing} directions */
    public static int[][][] vertexIndexToFaces = new int[6][6][6];
    
    private Field aoFaceVertexBrightness;
    private Field aoFaceVertexColorMultiplier;
    
    static {
        for (EnumFacing face : EnumFacing.values()) for (EnumFacing axis1 : EnumFacing.values()) for (EnumFacing axis2 : EnumFacing.values()) {
            vertexIndexToFaces[face.ordinal()][axis1.ordinal()][axis2.ordinal()] = BFAbstractRenderer.getAoIndexForFaces(face, axis1, axis2);
        }
    }
    
    public BlockShadingData(BFAbstractRenderer renderer) {
    	shadingBitSet.set(0);
        for (int j = 0; j < EnumFacing.values().length; ++j) aoFaces[j] = renderer.new BFAmbientOcclusionFace();
        
        try {
        	// This is gross. First, I have to use reflection to get the class that the vertex brightness and vertex
        	// color multiplier are stored in, because that class is package private and this class is in a different
        	// package. Then, I have to reflection on the class I got from reflection to get the actual field and
        	// whatnot. Dennis Ritchie, please forgive me
        	
        	Class<?>[] classesInBlockModelRenderer = BlockModelRenderer.class.getDeclaredClasses();
        	for(Class<?> cls : classesInBlockModelRenderer) {
        		if(cls.getName().contains("AmbientOcclusionFace")) {
        			// We've found the inner class we want
        			aoFaceVertexBrightness = cls.getDeclaredField("vertexBrightness");
        			aoFaceVertexColorMultiplier = cls.getDeclaredField("vertexColorMultiplier");
        			
        			break;	// Maybe could do this better?
        		}
        	}
        	
		} catch (NoSuchFieldException e) {
			LOG.log(Level.FATAL, "Could not find the field I wanted, cannot reasonably continue. Minecraft will crash", e);
		} catch (SecurityException e) {
			LOG.log(Level.FATAL, "Security mom is mad. Hiding the closet until the security dad comes home. Cannot reasonably continue. Minecraft will crash", e);
		}
    }
    
    /** Calculate shading data for the given block & position.
     * @param blockAccessIn world instance
     * @param blockIn block
     * @param blockPosIn block position
     * @param useAO true for ambient occlusion data, false for basic block brightness 
     */
    public void update(IBlockAccess blockAccessIn, Block blockIn, BlockPos blockPosIn, boolean useAO) {
        this.useAO = useAO;
        if (useAO) {
            // update ambient occlusion data
            for (EnumFacing facing : EnumFacing.values()) {
                aoFaces[facing.ordinal()].updateVertexBrightness(blockAccessIn, blockIn, blockPosIn, facing, null, shadingBitSet);
                isTranslucent[facing.ordinal()] = blockAccessIn.getBlockState(blockPosIn).getBlock().isTranslucent();
            }
        } else {
            // update basic brightness data
            for (EnumFacing facing : EnumFacing.values()) {
                BlockPos facingPos = blockPosIn.offset(facing);
                mixedBrightness[facing.ordinal()] = blockAccessIn.getBlockState(facingPos).getBlock().getMixedBrightnessForBlock(blockAccessIn, facingPos);
                isTranslucent[facing.ordinal()] = blockAccessIn.getBlockState(blockPosIn).getBlock().isTranslucent();
            }
        }
    }
    
    private int getAoFaceField(BFAmbientOcclusionFace face, Field fieldToGet, int index) {
    	try {
			int[] vertexBrightness = (int[]) fieldToGet.get(face);
			return vertexBrightness[index];
		} catch (Exception e) {
			LOG.log(Level.FATAL, "Something is terribly wrong. Debug plz", e);
		}
    	
    	return 1;
    }
    
    /* (non-Javadoc)
	 * @see mods.betterfoliage.client.render.IShadingData#getBrightness(net.minecraft.util.EnumFacing, net.minecraft.util.EnumFacing, net.minecraft.util.EnumFacing, boolean)
	 */
    @Override
	public int getBrightness(EnumFacing primary, EnumFacing secondary, EnumFacing tertiary, boolean useMax) {
        if (useAO) {
        	int index = vertexIndexToFaces[primary.ordinal()][secondary.ordinal()][tertiary.ordinal()];
        	
            int pri = getAoFaceField(aoFaces[primary.ordinal()], aoFaceVertexBrightness, index);
            if (!useMax) {
            	return pri;
            }
            
            int sec = getAoFaceField(aoFaces[secondary.ordinal()], aoFaceVertexBrightness, index);
            int ter = getAoFaceField(aoFaces[tertiary.ordinal()], aoFaceVertexBrightness, index);
            return pri > sec && pri > ter ? pri : (sec > ter ? sec : ter);
        } else {
            int pri = mixedBrightness[primary.ordinal()];
            if (!useMax) return pri;
            int sec = mixedBrightness[secondary.ordinal()];
            int ter = mixedBrightness[tertiary.ordinal()];
            return pri > sec && pri > ter ? pri : (sec > ter ? sec : ter);
        }
    }
    
    /* (non-Javadoc)
	 * @see mods.betterfoliage.client.render.IShadingData#getColorMultiplier(net.minecraft.util.EnumFacing, net.minecraft.util.EnumFacing, net.minecraft.util.EnumFacing, boolean)
	 */
    @Override
	public float getColorMultiplier(EnumFacing primary, EnumFacing secondary, EnumFacing tertiary, boolean useMax) {
    	int index = vertexIndexToFaces[primary.ordinal()][secondary.ordinal()][tertiary.ordinal()];
    	
        float pri = getAoFaceField(aoFaces[primary.ordinal()], aoFaceVertexColorMultiplier, index);
        if (!useMax) return pri;
        float sec = getAoFaceField(aoFaces[primary.ordinal()], aoFaceVertexColorMultiplier, index);
        float ter = getAoFaceField(aoFaces[primary.ordinal()], aoFaceVertexColorMultiplier, index);
        return pri > sec && pri > ter ? pri : (sec > ter ? sec : ter);
    }

	@Override
	public boolean shouldUseAO() {
		return useAO;
	}
}
