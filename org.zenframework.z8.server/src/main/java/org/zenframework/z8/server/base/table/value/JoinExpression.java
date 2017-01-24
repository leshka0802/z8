package org.zenframework.z8.server.base.table.value;

import org.zenframework.z8.server.base.query.Query;
import org.zenframework.z8.server.base.table.ITable;
import org.zenframework.z8.server.json.JsonWriter;
import org.zenframework.z8.server.runtime.IObject;
import org.zenframework.z8.server.types.sql.sql_bool;

public class JoinExpression extends Expression implements ILink {
	public static class CLASS<T extends JoinExpression> extends Expression.CLASS<T> {
		public CLASS(IObject container) {
			super(container);
			setJavaClass(JoinExpression.class);
			setSystem(true);
		}

		@Override
		public Object newObject(IObject container) {
			return new JoinExpression(container);
		}
	}

	private Query.CLASS<Query> query = null;

	public JoinExpression(IObject container) {
		super(container);
		setSystem(true);
	}

	@Override
	public Query.CLASS<Query> query() {
		return query;
	}

	@Override
	public Query getQuery() {
		return query != null ? query.get() : null;
	}

	@Override
	public ITable getReferencedTable() {
		Query query = getQuery();
		return query instanceof ITable ? (ITable)query : null;
	}

	@Override
	public IField getReferer() {
		return getQuery().primaryKey();
	}

	@Override
	public Join getJoin() {
		return Join.Left;
	}

	@Override
	public sql_bool on() {
		return new sql_bool(z8_expression());
	}

	protected sql_bool z8_expression() {
		return new sql_bool();
	}

	@Override
	public boolean isDataField() {
		return false;
	}

	@Override
	public void writeMeta(JsonWriter writer, Query query, Query context) {
		throw new UnsupportedOperationException();
	}

	@SuppressWarnings({ "unchecked", "rawtypes" })
	public void operatorAssign(Query.CLASS<? extends Query> data) {
		query = (Query.CLASS)data;
	}
}