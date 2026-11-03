package cz.nebixy.rainy.client.util;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;

import net.minecraft.client.Minecraft;

import cz.nebixy.rainy.RainyClient;

/**
 * Přístup k nastavením Minecraftu přes reflexi.
 *
 * Minecraft 26.x už není obfuskovaný, takže názvy metod jsou stejné ve vývoji i ve hře.
 * Když Mojang nějakou metodu přejmenuje, funkce se jen potichu vypne a zapíše do logu,
 * místo aby celý client spadl nebo nešel zkompilovat.
 */
public final class Compat {
	private Compat() {
	}

	private static final Map<String, Method> METHODS = new HashMap<>();
	private static final Map<String, Boolean> WARNED = new HashMap<>();

	private static Method method(Class<?> owner, String name, int params) {
		return method(owner, name, params, null);
	}

	/** Najde metodu podle jména, počtu parametrů a (u jednoho parametru) typu argumentu. */
	private static Method method(Class<?> owner, String name, int params, Class<?> argType) {
		String key = owner.getName() + "#" + name + "/" + params + (argType == null ? "" : ":" + argType.getName());
		if (METHODS.containsKey(key)) {
			return METHODS.get(key);
		}
		Method found = null;
		for (Class<?> c = owner; c != null && found == null; c = c.getSuperclass()) {
			for (Method m : c.getDeclaredMethods()) {
				if (!m.getName().equals(name) || m.getParameterCount() != params) {
					continue;
				}
				if (argType != null && !box(m.getParameterTypes()[0]).isAssignableFrom(argType)) {
					continue; // jiná varianta metody (např. getPlayerInfo(String) místo getPlayerInfo(UUID))
				}
				m.setAccessible(true);
				found = m;
				break;
			}
		}
		METHODS.put(key, found);
		return found;
	}

	private static Class<?> box(Class<?> type) {
		if (!type.isPrimitive()) {
			return type;
		}
		if (type == boolean.class) return Boolean.class;
		if (type == int.class) return Integer.class;
		if (type == double.class) return Double.class;
		if (type == float.class) return Float.class;
		if (type == long.class) return Long.class;
		return type;
	}

	private static void warn(String what, Throwable t) {
		if (WARNED.putIfAbsent(what, Boolean.TRUE) == null) {
			RainyClient.LOGGER.warn("[Rainy] {} není v této verzi dostupné: {}", what, t == null ? "metoda nenalezena" : t.toString());
		}
	}

	/** Zavolá metodu bez parametrů, při chybě vrátí null. */
	public static Object call(Object target, String name) {
		if (target == null) {
			return null;
		}
		try {
			Method m = method(target.getClass(), name, 0);
			if (m == null) {
				warn(target.getClass().getSimpleName() + "." + name + "()", null);
				return null;
			}
			return m.invoke(target);
		} catch (Throwable t) {
			warn(target.getClass().getSimpleName() + "." + name + "()", t);
			return null;
		}
	}

	/** Zavolá metodu s jedním parametrem, při chybě vrátí null. */
	public static Object call(Object target, String name, Object arg) {
		if (target == null) {
			return null;
		}
		try {
			Method m = method(target.getClass(), name, 1, arg == null ? null : arg.getClass());
			if (m == null) {
				warn(target.getClass().getSimpleName() + "." + name + "(x)", null);
				return null;
			}
			return m.invoke(target, arg);
		} catch (Throwable t) {
			warn(target.getClass().getSimpleName() + "." + name + "(x)", t);
			return null;
		}
	}

	// ---------------------------------------------------------------- nastavení (OptionInstance)

	private static Object options() {
		return Minecraft.getInstance().options;
	}

	/** Vrátí aktuální hodnotu nastavení, např. option("fov") nebo option("entityShadows"). */
	public static Object getOption(String name) {
		Object instance = call(options(), name);
		return instance == null ? null : call(instance, "get");
	}

	/** Nastaví hodnotu nastavení. Vrací true, když se to povedlo. */
	public static boolean setOption(String name, Object value) {
		Object instance = call(options(), name);
		if (instance == null || value == null) {
			return false;
		}
		try {
			Method set = method(instance.getClass(), "set", 1);
			if (set == null) {
				warn("OptionInstance.set", null);
				return false;
			}
			set.invoke(instance, value);
			return true;
		} catch (Throwable t) {
			warn("Nastavení " + name, t);
			return false;
		}
	}

	private static final Map<Class<?>, Field> VALUE_FIELDS = new HashMap<>();

	/**
	 * Nastaví hodnotu nastavení napřímo, bez kontroly rozsahu (např. jas 16.0, i když posuvník jde jen do 1.0).
	 */
	public static boolean setOptionRaw(String name, Object value) {
		Object instance = call(options(), name);
		if (instance == null || value == null) {
			return false;
		}
		try {
			Field f = valueField(instance);
			if (f == null) {
				warn("Hodnota nastavení " + name, null);
				return false;
			}
			f.set(instance, value);
			return true;
		} catch (Throwable t) {
			warn("Hodnota nastavení " + name, t);
			return false;
		}
	}

	/** Najde pole, ve kterém OptionInstance drží svoji hodnotu (normálně se jmenuje "value"). */
	private static Field valueField(Object instance) throws IllegalAccessException {
		Class<?> type = instance.getClass();
		if (VALUE_FIELDS.containsKey(type)) {
			return VALUE_FIELDS.get(type);
		}
		Object current = call(instance, "get");
		Field byName = null;
		Field byValue = null;
		for (Class<?> c = type; c != null; c = c.getSuperclass()) {
			for (Field f : c.getDeclaredFields()) {
				if (java.lang.reflect.Modifier.isStatic(f.getModifiers()) || f.getType().isPrimitive()) {
					continue;
				}
				f.setAccessible(true);
				if (f.getName().equals("value")) {
					byName = f;
				} else if (byValue == null && current != null && f.getType().isAssignableFrom(current.getClass()) && current.equals(f.get(instance))) {
					byValue = f;
				}
			}
		}
		Field found = byName != null ? byName : byValue;
		VALUE_FIELDS.put(type, found);
		return found;
	}

	/**
	 * Převede text zpět na hodnotu stejného typu, jako má aktuální hodnota (Boolean, Integer, Double, enum).
	 */
	@SuppressWarnings({"unchecked", "rawtypes"})
	public static Object parseLike(Object current, String text) {
		if (current == null || text == null) {
			return null;
		}
		try {
			if (current instanceof Boolean) {
				return Boolean.parseBoolean(text);
			}
			if (current instanceof Integer) {
				return Integer.parseInt(text);
			}
			if (current instanceof Double) {
				return Double.parseDouble(text);
			}
			if (current instanceof Enum<?> e) {
				return Enum.valueOf((Class) e.getDeclaringClass(), text);
			}
		} catch (Throwable t) {
			warn("Převod hodnoty " + text, t);
		}
		return null;
	}

	/** Najde konstantu enumu podle jména, se stejným typem jako aktuální hodnota (např. "MINIMAL"). */
	public static Object enumLike(Object current, String constant) {
		return current instanceof Enum<?> ? parseLike(current, constant) : null;
	}

	// ---------------------------------------------------------------- pole (fields)

	public static boolean getBooleanField(Object target, String name) {
		try {
			Field f = target.getClass().getField(name);
			return f.getBoolean(target);
		} catch (Throwable t) {
			warn("Pole " + name, t);
			return false;
		}
	}

	public static void setBooleanField(Object target, String name, boolean value) {
		try {
			Field f = target.getClass().getField(name);
			f.setBoolean(target, value);
		} catch (Throwable t) {
			warn("Pole " + name, t);
		}
	}

	// ---------------------------------------------------------------- hra

	/** Ping hráče v ms, nebo -1 když to nejde zjistit (třeba v singleplayeru). */
	public static int ping() {
		Minecraft mc = Minecraft.getInstance();
		if (mc.player == null) {
			return -1;
		}
		Object info = call(mc.player.connection, "getPlayerInfo", mc.player.getUUID());
		Object latency = call(info, "getLatency");
		return latency instanceof Integer i ? i : -1;
	}

	/** Po změně některých grafických nastavení se musí přestavět chunky. */
	public static void reloadChunks() {
		Minecraft mc = Minecraft.getInstance();
		if (mc.level == null) {
			return;
		}
		try {
			mc.levelExtractor.allChanged(); // jako F3 + A
		} catch (Throwable t) {
			warn("levelExtractor.allChanged", t);
		}
	}
}
