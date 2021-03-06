package org.zenframework.z8.server.db.sql.functions;

import java.util.Collection;

import org.zenframework.z8.server.base.table.value.IField;
import org.zenframework.z8.server.db.DatabaseVendor;
import org.zenframework.z8.server.db.FieldType;
import org.zenframework.z8.server.db.sql.FormatOptions;
import org.zenframework.z8.server.db.sql.SqlToken;
import org.zenframework.z8.server.db.sql.functions.conversion.ToString;
import org.zenframework.z8.server.exceptions.db.UnknownDatabaseException;

public class Window extends SqlToken {
	private SqlToken token;
	private String name;

	protected Window(SqlToken token, String name) {
		this.token = token;
		this.name = name;
	}

	@Override
	public void collectFields(Collection<IField> fields) {
		token.collectFields(fields);
	}

	@Override
	public String format(DatabaseVendor vendor, FormatOptions options, boolean logicalContext) throws UnknownDatabaseException {
		String result = options.isAggregationEnabled() ? name + "(" : "";

		FieldType type = token.type();

		options.disableAggregation();

		SqlToken token = this.token;

		switch(vendor) {
		case Postgres:
		case Oracle:
			if(type == FieldType.Guid || type == FieldType.Text || type == FieldType.Attachments || type == FieldType.File)
				token = new ToString(token);
			break;
		default:
		}

		result += token.format(vendor, options);
		options.enableAggregation();

		result += options.isAggregationEnabled() ? ")" : "";

		switch(vendor) {
		case Postgres:
			if(type == FieldType.Guid)
				result += "::uuid";
			break;
		case Oracle:
			if(type == FieldType.Guid)
				result = "HEXTORAW(" + result + ")";
			break;
		default:
		}

		return result;
	}

	@Override
	public FieldType type() {
		return token.type();
	}
}
