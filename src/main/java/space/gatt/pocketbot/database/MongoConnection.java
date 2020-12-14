package space.gatt.pocketbot.database;

import com.mongodb.MongoClient;
import com.mongodb.MongoClientOptions;
import com.mongodb.MongoCredential;
import com.mongodb.ServerAddress;
import dev.morphia.Datastore;
import dev.morphia.Morphia;
import dev.morphia.mapping.MapperOptions;
import dev.morphia.query.FindOptions;
import dev.morphia.query.Query;
import dev.morphia.query.internal.MorphiaCursor;
import space.gatt.pocketbot.PocketBotMain;
import space.gatt.pocketbot.database.interfaces.MorphiaHelper;

import java.io.IOException;
import java.net.URL;
import java.security.CodeSource;
import java.util.*;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

public class MongoConnection {

	private MongoClient client;
	private Morphia morphia;

	private HashMap<String, Datastore> datastoreStorage = new HashMap<>();
	private HashMap<Class, Datastore> classDatastoreStorage = new HashMap<>();

	public MongoConnection(String ip, Integer port, String username, String password) {
		MongoCredential credential = MongoCredential.createCredential(username, "admin", password.toCharArray());
		ServerAddress addr = new ServerAddress(ip, port);
		MongoClientOptions options = new MongoClientOptions.Builder().sslEnabled(false).applicationName(PocketBotMain.getInstance().getJDAInstance().getSelfUser().getName()).retryWrites(true).build();
		client = new MongoClient(addr, credential, options);
		morphia = new Morphia();

		morphia.getMapper().setOptions(MapperOptions.builder(morphia.getMapper().getOptions()).ignoreFinals(true).storeEmpties(false).storeNulls(false)
				.mapSubPackages(false).cacheClassLookups(true).build());
	}

	public MongoClient getMongoClient() {
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
							morphia.getMapper().addMappedClass(c);
						} catch (Throwable ignore) {
						}
						ds.ensureIndexes(c);

						System.out.println("[MORPHIA] Mapped Class " + c.getName() + " for Database " + mh.datastore() + (PocketBotMain.getInstance().getBotConfiguration().isDevMode() ? "_dev" : ""));
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


		Datastore store = morphia.createDatastore(client, datastore);
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
			Query<T> query = store.createQuery(clz).disableValidation().filter(key, value);

			if (query.count() > 0) {
				Object obj = query.first();
				if (obj.getClass() == clz) {
					return clz.cast(obj);
				}
			}

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
			MorphiaCursor<T> cursor = store.createQuery(clz).filter(key, value).find(new FindOptions().limit(limit));
			while (cursor.hasNext()) {
				Object ob = cursor.next();
				if (ob.getClass() == clz) {
					objects.add(clz.cast(ob));
				}
			}
			return objects;
		}
		return null;
	}

	public synchronized <T> List<T> getMultipleObjects(Integer limit, Class<T> clz) {
		if (classDatastoreStorage.containsKey(clz)) {
			Datastore store = classDatastoreStorage.get(clz);
			List<T> objects = new ArrayList<>();
			MorphiaCursor<T> cursor = store.createQuery(clz).find(new FindOptions().limit(limit));
			while (cursor.hasNext()) {
				Object ob = cursor.next();
				if (ob.getClass() == clz) {
					objects.add(clz.cast(ob));
				}
			}
			return objects;
		}
		return null;
	}

}
