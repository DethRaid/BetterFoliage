package mods.betterfoliage.client.misc;

import java.lang.reflect.Type;

import mods.betterfoliage.BetterFoliage;
import net.minecraft.client.resources.data.IMetadataSection;
import net.minecraft.client.resources.data.IMetadataSectionSerializer;
import net.minecraft.util.JsonUtils;

import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonElement;
import com.google.gson.JsonParseException;
import com.google.gson.JsonSyntaxException;

public class BetterFoliageMetadataSection implements IMetadataSection {

	public boolean rotation;
	
	public static class BetterFoliageMetadataSerializer implements IMetadataSectionSerializer {

		@Override
		public String getSectionName() {
			// I'm not sure what you're trying to do here, but you have complete control of the BetterFoliage class so
			// you can fix it :P
			return BetterFoliage.METADATA_SECTION;
		}

		@Override
		public BetterFoliageMetadataSection deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context) throws JsonParseException {
			BetterFoliageMetadataSection result = new BetterFoliageMetadataSection();
			
			boolean rotationFromJson;
			try {
				rotationFromJson = JsonUtils.getBoolean(json.getAsJsonObject(), "rotation");
			} catch(JsonSyntaxException e ) {
				rotationFromJson = true;
			}
			
			result.rotation = rotationFromJson;
			return result;
		}

	}
}
