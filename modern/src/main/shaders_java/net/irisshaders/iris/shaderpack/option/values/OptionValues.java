package net.irisshaders.iris.shaderpack.option.values;

import static net.irisshaders.iris.IrisLogging.IrisLogger;
import net.irisshaders.iris.helpers.OptionalBoolean;
import net.irisshaders.iris.shaderpack.option.OptionSet;

import java.util.Optional;

public interface OptionValues {
	OptionalBoolean getBooleanValue(String name);

	Optional<String> getStringValue(String name);

	default boolean getBooleanValueOrDefault(String name) {
		return getBooleanValue(name).orElseGet(() -> {
			if (!getOptionSet().getBooleanOptions().containsKey(name)) {
				IrisLogger.warn("Tried to get boolean value for unknown option: " + name + ", defaulting to true!");
				return true;
			}
			return getOptionSet().getBooleanOptions().get(name).getOption().getDefaultValue();
		});
	}

	default String getStringValueOrDefault(String name) {
		return getStringValue(name).orElseGet(() -> getOptionSet().getStringOptions().get(name).getOption().getDefaultValue());
	}

	int getOptionsChanged();

	MutableOptionValues mutableCopy();

	ImmutableOptionValues toImmutable();

	OptionSet getOptionSet();
}
