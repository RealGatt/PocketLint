package space.gatt.pocketbot.database;

import com.mongodb.*;
import com.mongodb.client.MongoClients;
import dev.morphia.Datastore;
import dev.morphia.Morphia;
import dev.morphia.mapping.MapperOptions;
import dev.morphia.query.FindOptions;
import space.gatt.pocketbot.PocketBotMain;
import space.gatt.pocketbot.database.interfaces.MorphiaHelper;

import java.io.IOException;
import java.net.URL;
import java.security.CodeSource;
import java.util.*;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

import static dev.morphia.query.experimental.filters.Filters.eq;

public class MongoConnection {

	private com.mongodb.client.MongoClient client;

	private HashMap<String, Datastore> datastoreStorage = new HashMap<>();
	private HashMap<Class, Datastore> classDatastoreStorage = new HashMap<>();
	private MapperOptions options;

	public MongoConnection(String uri) {
		client = MongoClients.create(new ConnectionString(uri));
		options = MapperOptions.builder().ignoreFinals(true).storeEmpties(false).storeNulls(false)
				.mapSubPackages(true).cacheClassLookups(true).build();
	}

	public com.mongodb.client.MongoClient getMongoClient() {
		return client;
	}

	private Collection<Class<?>> getClassesInPackage(PocketBotMain plugin, String packageName) {
		Collection<Class<?>> classes = new ArrayList<>();

		CodeSource codeSource = plugin.getClass().getProtectionDomain().getCodeSource();
		URL resource = codeSource.getLocation();
		String relPath = packageName.replace('.', '/');
		String resPath = resource.getPath().replace("%20", " ");
		String jarPath = resPath.replaceFirst("[.]jar[!].*", ".jar").replaceFirst("file:", "");
		JarFile jarFile;

		try {
			jarFile = new JarFile(jarPath);
		} catch (IOException e) {
			throw (new RuntimeException("Unexpected IOException reading JAR File '" + jarPath + "'", e));
		}

		Enumeration<JarEntry> entries = jarFile.entries();

		while (entries.hasMoreElements()) {
			JarEntry entry = entries.nextElement();
			String entryName = entry.getName();
			String className = null;

			if (entryName.endsWith(".class") && entryName.startsWith(relPath) &&
					entryName.length() > (relPath.length() + "/".length())) {
				className = entryName.replace('/', '.').replace('\\', '.').replace(".class", "");
			}

			if (className != null) {
				Class<?> clazz = null;

				try {
					clazz = Class.forName(className);
				} catch (ClassNotFoundException | NoClassDefFoundError ignored) {
				}

				if (clazz != null) {
					classes.add(clazz);
				}
			}
		}

		try {
			jarFile.close();
		} catch (IOException e) {
			e.printStackTrace();
		}

		return classes;
	}

	public void registerMorphiaMaps(PocketBotMain plugin, String path) {
		try {

			getClassesInPackage(plugin, path).forEach(c -> {
				if (c.isAnnotationPresent(MorphiaHelper.class)) {
					MorphiaHelper mh = c.getAnnotation(MorphiaHelper.class);
					try {

						System.out.println("[MORPHIA] Mapping Class " + c.getName());
						Datastore ds = getDatastore(mh.datastore() + (PocketBotMain.getInstance().getBotConfiguration().isDevMode() ? "_dev" : ""));
						classDatastoreStorage.put(c, ds);

						try {
							ds.getMapper().map(c);

							ds.ensureIndexes(c);

							System.out.println("[MORPHIA] Mapped Class " + c.getName() + " for Database " + mh.datastore() + (PocketBotMain.getInstance().getBotConfiguration().isDevMode() ? "_dev" : ""));
						} catch (Throwable thr) {
							thr.printStackTrace();
						}
					} catch (Exception e) {
						classDatastoreStorage.remove(c);
						System.out.println("[MORPHIA] Failed to Map Class " + c.getName());
						e.printStackTrace();
					}
				}
			});
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	public synchronized Datastore getDatastore(String datastore) {
		datastore = datastore.toLowerCase();

		if (datastoreStorage.containsKey(datastore))
			return datastoreStorage.get(datastore);


		Datastore store = Morphia.createDatastore((com.mongodb.client.MongoClient) client, datastore, options);
		datastoreStorage.put(datastore, store);

		return store;
	}

	public synchronized boolean storeObject(Object obj) {
		try {
			Class clz = obj.getClass();
			Datastore store = classDatastoreStorage.get(clz);
			store.save(obj);
			return true;
		} catch (Exception e) {
			e.printStackTrace();
			return false;
		}
	}

	public synchronized <T> T getSingleObject(String key, Object value, Class<T> clz, boolean createNewInstance) {
		if (classDatastoreStorage.containsKey(clz)) {
			Datastore store = classDatastoreStorage.get(clz);
			T obj = store.find(clz).filter(eq(key, value)).first();
			if (obj != null) return obj;
		}
		if (createNewInstance) {
			try {
				return clz.newInstance();
			} catch (Exception e) {
				return null;
			}
		}
		return null;
	}

	public synchronized <T> T getSingleObject(String key, Object value, Class<T> clz) {
		return getSingleObject(key, value, clz, false);
	}

	public synchronized <T> List<T> getMultipleObjects(String key, Object value, Integer limit, Class<T> clz) {
		if (classDatastoreStorage.containsKey(clz)) {
			Datastore store = classDatastoreStorage.get(clz);
			List<T> objects = new ArrayList<>();
			store.find(clz).filter(eq(key, value)).iterator(new FindOptions().limit(limit)).forEachRemaining((ob)->{
				if (ob.getClass() == clz) {
					objects.add(clz.cast(ob));
				}
			});
			return objects;
		}
		return null;
	}

	public synchronized <T> List<T> getMultipleObjects(Integer limit, Class<T> clz) {
		if (classDatastoreStorage.containsKey(clz)) {
			Datastore store = classDatastoreStorage.get(clz);
			List<T> objects = new ArrayList<>();
			store.find(clz).iterator(new FindOptions().limit(limit)).forEachRemaining((ob)->{
				if (ob.getClass() == clz) {
					objects.add(clz.cast(ob));
				}
			});
			return objects;
		}
		return null;
	}

}
