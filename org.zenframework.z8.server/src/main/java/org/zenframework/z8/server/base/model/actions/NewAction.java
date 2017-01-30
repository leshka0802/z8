package org.zenframework.z8.server.base.model.actions;

import java.sql.SQLException;
import java.util.Collection;

import org.zenframework.z8.server.base.model.sql.Select;
import org.zenframework.z8.server.base.query.Query;
import org.zenframework.z8.server.base.table.value.Field;
import org.zenframework.z8.server.base.table.value.Link;
import org.zenframework.z8.server.json.Json;
import org.zenframework.z8.server.json.JsonWriter;
import org.zenframework.z8.server.types.guid;
import org.zenframework.z8.server.types.primary;

public class NewAction extends RequestAction {
	public NewAction(ActionConfig config) {
		super(config);
	}

	@Override
	public void writeResponse(JsonWriter writer) throws Throwable {
		Query query = getQuery();

		guid recordId = guid.create();
		guid parentId = getParentIdParameter();

		Collection<Field> fields = run(query, recordId, parentId);

		initLinks(fields);

		writer.startArray(Json.data);

		writer.startObject();
		for(Field field : fields)
			field.writeData(writer);
		writer.finishObject();

		writer.finishArray();
	}

	private void initLinks(Collection<Field> fields) throws SQLException {
		for(Field field : fields.toArray(new Field[0])) // ConcurrentModification
		{
			if(field instanceof Link) {
				Link link = (Link)field;
				guid recordId = link.guid();

				if(!recordId.equals(guid.Null)) {
					Query query = link.getQuery();

					ActionConfig config = config();
					Collection<Field> queryFields = config.fields != null ? config.fields : query.fields();
					queryFields = query.getReachableFields(queryFields);

					if(queryFields.size() != 0) {
						ReadAction action = new ReadAction(query, queryFields);
						action.addFilter(query.primaryKey(), recordId);

						Select cursor = action.getCursor();

						try {
							if(cursor.next()) {
								for(Field actionField : action.selectFields) {
									actionField.set(actionField.get());

									if(!fields.contains(actionField))
										fields.add(actionField);
								}
							}
						} finally {
							cursor.close();
						}
					}
				}
			}
		}
	}

	static private Collection<Field> initFields(Collection<Field> fields, guid recordId, guid parentId) {
		for(Field field : fields) {
			if(field.changed())
				continue;

			if(field.isPrimaryKey()) {
				field.set(recordId);
			} else if(field.isParentKey() && parentId != null) {
				field.set(parentId);
			} else {
				primary value = field.getDefault();
				if(!value.equals(field.getDefaultValue()))
					field.set(value);
			}
		}

		return fields;
	}

	static public Collection<Field> run(Query query, guid recordId, guid parentId) {
		Collection<Field> fields = query.getPrimaryFields();

		query.onNew(recordId, parentId != null ? parentId : guid.Null);

		initFields(fields, recordId, parentId);

		return fields;
	}
}
