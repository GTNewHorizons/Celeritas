package net.irisshaders.iris.texture.pbr;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;
import org.apache.commons.io.FilenameUtils;
import org.jetbrains.annotations.Nullable;

import java.util.function.Predicate;

public enum PBRType {
	NORMAL("_n", 0x7F7FFFFF),
	SPECULAR("_s", 0x00000000);

	// Many mods use "_n"/"_s"/"_e"/"_w" as cardinal-direction (north/south/east/west) suffixes for
	// oriented block faces. Those textures aren't PBR maps, but "_n"/"_s" collide with our suffixes.
	// If a file sitting next to a candidate PBR suffix has a sibling using one of the other compass
	// directions, treat the whole group as a directional texture set instead of a PBR pair - genuine
	// PBR resource packs essentially never ship "_e"/"_w" companions.
	private static final String[] NON_PBR_DIRECTIONAL_SUFFIXES = {"_e", "_w"};

	private static final PBRType[] VALUES = values();

	private final String suffix;
	private final int defaultValue;

	PBRType(String suffix, int defaultValue) {
		this.suffix = suffix;
		this.defaultValue = defaultValue;
	}

	@Nullable
	public static String removeSuffix(String path) {
		int extensionIndex = FilenameUtils.indexOfExtension(path);
		String pathNoExtension = path.substring(0, extensionIndex);
		PBRType type = fromFileLocation(pathNoExtension);
		if (type != null) {
			String suffix = type.getSuffix();
			String basePathNoExtension = pathNoExtension.substring(0, pathNoExtension.length() - suffix.length());
			return basePathNoExtension + path.substring(extensionIndex);
		}
		return null;
	}

	/**
	 * Returns the PBR type corresponding to the suffix of the given file location.
	 *
	 * @param location The file location without an extension
	 * @return the PBR type
	 */
	@Nullable
	public static PBRType fromFileLocation(String location) {
		for (PBRType type : VALUES) {
			if (location.endsWith(type.getSuffix())) {
				return type;
			}
		}
		return null;
	}

	/**
	 * Checks whether the given (PBR-suffixed) location has a sibling texture using one of the other
	 * cardinal-direction suffixes, e.g. whether "block/buffer_side_n.png" has a "block/buffer_side_e.png"
	 * next to it. If so, "location" is almost certainly part of a directional texture set rather than an
	 * actual PBR map, and should not be treated as one.
	 *
	 * @param location The full location of the candidate PBR texture (with extension)
	 * @param exists Checks whether a candidate sibling location actually exists
	 * @return true if a directional sibling was found
	 */
	public static boolean hasDirectionalSiblings(ResourceLocation location, Predicate<ResourceLocation> exists) {
		String path = location.getPath();
		int extensionIndex = FilenameUtils.indexOfExtension(path);
		String pathNoExtension = extensionIndex != -1 ? path.substring(0, extensionIndex) : path;
		String extension = extensionIndex != -1 ? path.substring(extensionIndex) : "";

		PBRType type = fromFileLocation(pathNoExtension);
		if (type == null) {
			return false;
		}

		String basePathNoExtension = pathNoExtension.substring(0, pathNoExtension.length() - type.getSuffix().length());
		for (String siblingSuffix : NON_PBR_DIRECTIONAL_SUFFIXES) {
			ResourceLocation sibling = location.withPath(basePathNoExtension + siblingSuffix + extension);
			if (exists.test(sibling)) {
				return true;
			}
		}

		return false;
	}

	public static boolean hasDirectionalSiblings(ResourceLocation location, ResourceManager resourceManager) {
		return hasDirectionalSiblings(location, sibling -> resourceManager.getResource(sibling).isPresent());
	}

	public String getSuffix() {
		return suffix;
	}

	public int getDefaultValue() {
		return defaultValue;
	}

	public String appendSuffix(String path) {
		int extensionIndex = FilenameUtils.indexOfExtension(path);
		if (extensionIndex != -1) {
			return path.substring(0, extensionIndex) + suffix + path.substring(extensionIndex);
		} else {
			return path + suffix;
		}
	}
}
