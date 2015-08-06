package org.zenframework.z8.server.base.table.system;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map;

import org.zenframework.z8.server.base.query.Query;
import org.zenframework.z8.server.base.table.Table;
import org.zenframework.z8.server.base.table.TreeTable;
import org.zenframework.z8.server.base.table.value.StringField;
import org.zenframework.z8.server.db.generator.DBGenerator;
import org.zenframework.z8.server.db.sql.SqlToken;
import org.zenframework.z8.server.db.sql.expressions.Operation;
import org.zenframework.z8.server.db.sql.expressions.Or;
import org.zenframework.z8.server.db.sql.expressions.Rel;
import org.zenframework.z8.server.engine.ApplicationServer;
import org.zenframework.z8.server.engine.Runtime;
import org.zenframework.z8.server.logs.Trace;
import org.zenframework.z8.server.resources.Resources;
import org.zenframework.z8.server.runtime.IObject;
import org.zenframework.z8.server.runtime.RCollection;
import org.zenframework.z8.server.runtime.RLinkedHashMap;
import org.zenframework.z8.server.types.bool;
import org.zenframework.z8.server.types.guid;
import org.zenframework.z8.server.types.string;
import org.zenframework.z8.server.types.sql.sql_string;

public class Properties extends TreeTable {

    public static final String TableName = "SystemProperties";
    public static final int PropertyNameMaxLength = 256;
    public static final int PropertyValueMaxLength = 1024;

    private static final Collection<Listener> Listeners = Collections.synchronizedCollection(new LinkedList<Listener>());

    public static interface Listener {

        void onPropertyChange(String key, String value);

    }

    public static void addListener(Listener listener) {
        Listeners.add(listener);
    }

    public static void removeListener(Listener listener) {
        Listeners.remove(listener);
    }

    static public class strings {
        public final static String Title = "Properties.title";
        public final static String Value = "Properties.value";
    }

    public static class CLASS<T extends Properties> extends Table.CLASS<T> {

        public CLASS() {
            this(null);
        }

        public CLASS(IObject container) {
            super(container);
            setJavaClass(Properties.class);
            setName(TableName);
            setDisplayName(Resources.get(strings.Title));
            DBGenerator.addListener(new DbGeneratorListener());
        }

        @Override
        public Object newObject(IObject container) {
            return new Properties(container);
        }

    }

    public final StringField.CLASS<StringField> value = new StringField.CLASS<StringField>(this);

    private Properties(IObject container) {
        super(container);
    }

    @Override
    public void constructor2() {
        super.constructor2();
        id.get().length.set(PropertyNameMaxLength);
        id1.get().visible = new bool(false);
        name.get().visible = new bool(false);
        name.get().length.set(PropertyNameMaxLength);
        value.setName("Value");
        value.setIndex("value");
        value.setDisplayName(Resources.get(strings.Value));
        value.setGendb_updatable(false);
        value.get().length.set(PropertyValueMaxLength);
        registerDataField(value);
    }

    public static void setProperty(String key, String value) {
        if (ApplicationServer.defaultDatabase().isSystemInstalled()) {
            Properties properties = new CLASS<Properties>().get();
            if (!properties.readFirst(properties.getWhere(key))) {
                throw new RuntimeException("Property [" + key + "] is not registered");
            } else {
                properties.value.get().set(value);
                properties.update(properties.recordId());
            }
        } else {
            throw new RuntimeException("Can not set property [" + key + "=" + value + "]. System is not installed");
        }
    }

    public static String getProperty(String key) {
        String dbValue = getPropertyFromDb(key);
        if (dbValue == null) {
            throw new RuntimeException("Property [" + key + "] does not exists");
        } else {
            return dbValue;
        }
    }

    public static String getProperty(String key, String defaultValue) {
        String dbValue = getPropertyFromDb(key);
        return dbValue == null ? defaultValue : dbValue;
    }

    public static String getProperty(Property property) {
        return getProperty(property.getKey(), property.getDefaultValue());
    }

    public static Map<String, String> getProperties() {
        Map<String, String> values = new HashMap<String, String>();
        for (Property property : Runtime.instance().properties()) {
            values.put(property.getKey(), property.getDefaultValue());
        }
        if (ApplicationServer.defaultDatabase().isSystemInstalled()) {
            Properties properties = new CLASS<Properties>().get();
            properties.read();
            while (properties.next()) {
                values.put(properties.id.get().string().get(), properties.value.get().string().get());
            }
        }
        return values;
    }

    public static void z8_setProperty(string key, string value) {
        setProperty(key.get(), value.get());
    }

    public static string z8_getProperty(string key) {
        return new string(getProperty(key.get()));
    }

    public static string z8_getProperty(string key, string defaultValue) {
        return new string(getProperty(key.get(), defaultValue.get()));
    }

    public static string z8_getProperty(Property.CLASS<Property> property) {
        return new string(getProperty(property.get()));
    }

    public static RLinkedHashMap<string, string> z8_getProperties() {
        RLinkedHashMap<string, string> props = new RLinkedHashMap<string, string>();
        for (Map.Entry<String, String> property : getProperties().entrySet()) {
            props.put(new string(property.getKey()), new string(property.getValue()));
        }
        return props;
    }

    @SuppressWarnings("rawtypes")
    @Override
    public void z8_afterUpdate(Query.CLASS<? extends Query> query, guid recordId, RCollection changedFields,
            Query.CLASS<? extends Query> model, guid modelRecordId) {
        if (changedFields.contains(value) && readRecord(recordId)) {
            firePropertyChange(id.get().string().get(), value.get().string().get());
        }
    }

    private SqlToken getWhere(String key) {
        return new Or(new Rel(id.get(), Operation.Eq, new sql_string(key)), new Rel(name.get(), Operation.Eq, new sql_string(key)));
    }

    private guid getParentId(Map<String, guid> ids, String key) {
        int pos = key.lastIndexOf('.');
        if (pos < 0)
            return guid.NULL;
        String parentKey = key.substring(0, pos);
        if (ids.containsKey(parentKey))
            return ids.get(parentKey);
        guid parentRecordId = guid.create(0, parentKey.hashCode());
        boolean exists = hasRecord(parentRecordId);
        guid grandParentRecordId = getParentId(ids, parentKey);
        id.get().set(parentKey);
        name.get().set(parentKey);
        description.get().set("");
        value.get().set("");
        parentId.get().set(grandParentRecordId);
        if (!exists) {
            recordId.get().set(parentRecordId);
            create();
        } else {
            update(parentRecordId);
        }
        ids.put(parentKey, parentRecordId);
        return parentRecordId;
    }

    private static String getPropertyFromDb(String key) {
        try {
            if (ApplicationServer.defaultDatabase().isSystemInstalled()) {
                Properties properties = new CLASS<Properties>().get();
                if (properties.readFirst(properties.getWhere(key))) {
                    return properties.value.get().string().get();
                }
            }
        } catch (Throwable e) {
            Trace.logError("Can't read property '" + key + "' from database", e);
        }
        return null;
    }

    private static void firePropertyChange(String key, String value) {
        Collection<Listener> listeners = new ArrayList<Properties.Listener>(Listeners);
        for (Listener listener : listeners) {
            listener.onPropertyChange(key, value);
        }
    }

    private static class DbGeneratorListener implements DBGenerator.Listener {

        @Override
        public void onDbGenerated() {
            Properties properties = new CLASS<Properties>().get();
            Map<String, guid> ids = new HashMap<String, guid>();
            for (Property property : Runtime.instance().properties()) {
                boolean exists = properties.readRecord(property.getId());
                guid parentId = properties.getParentId(ids, property.getKey());
                properties.id.get().set(property.getKey());
                properties.name.get().set(property.getKey());
                properties.description.get().set(property.getDescription());
                properties.parentId.get().set(parentId);
                if (!exists) {
                    properties.recordId.get().set(property.getId());
                    properties.value.get().set(property.getDefaultValue());
                    properties.create();
                } else {
                    properties.update(property.getId());
                }
            }
        }

    }

}
