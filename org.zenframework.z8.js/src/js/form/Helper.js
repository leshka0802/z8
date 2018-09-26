Z8.define('Z8.form.Helper', {
	statics: {
		storeConfig: function(field) {
			return Z8.query.Store.config(field);
		},

		createControl: function(field) {
			if(field == null)
				return null;

			var readOnly = field.readOnly;
			var label = field.label !== false ? { text: field.header, icon: field.icon, align: 'top' } : false;
			var config = { label: label, placeholder: field.header, name: field.name, field: field, colSpan: field.colSpan, flex: field.flex, readOnly: readOnly, editable: field.editable, required: field.required, enterOnce: field.enterOnce, source: field.source };

			if(field.cls != null)
				config.cls = field.cls;

			if(field.validation != null)
				config.validation = field.validation;

			if(field.value != null)
				config.value = field.value;

			if(field.displayValue != null)
				config.displayValue = field.displayValue;

			if(field.format != null)
				config.format = field.format;

			var cls = Application.getSubclass(field.ui);

			if(field.isCombobox) {
				var link = field.link;
				config.store = this.storeConfig(field);
				config.displayName = link == null ? field.displayName || field.name : field.name;
				config.fields = field;
				config.name = link != null ? link.name : field.name;
				config.checks = false;
				config.pagerMode = 'visible';
				return cls != null ? Z8.create(cls, config) : (field.isSearch ? new Z8.form.field.SearchCombobox(config) : new Z8.form.field.Combobox(config));
			}

			if(field.isListbox) {
				var query = field.query;
				config.store = this.storeConfig(field);
				config.query = query;
				config.fields = query.columns;
				config.tools = true;
				config.pagerMode = 'visible';
				config.checks = Application.listbox.checks !== false;
				config.locks = Application.listbox.locks !== false && query.lockKey != null;
				config.totals = query.totals;
				config.minHeight = config.height = Ems.unitsToEms(field.height || 5);
				return cls != null ? Z8.create(cls, config) : new Z8.form.field.Listbox(config);
			}

			var type = field.type;

			if(type == Type.Boolean) {
				label.align = 'right';
				return cls != null ? Z8.create(cls, config) : new Z8.form.field.Checkbox(config);
			}

			if(type == Type.Integer)
				return cls != null ? Z8.create(cls, config) : new Z8.form.field.Integer(config);

			if(type == Type.Float)
				return cls != null ? Z8.create(cls, config) : new Z8.form.field.Float(config);

			if(type == Type.Date)
				return cls != null ? Z8.create(cls, config) : new Z8.form.field.Date(config);

			if(type == Type.Datetime)
				return cls != null ? Z8.create(cls, config) : new Z8.form.field.Datetime(config);

			if(type == Type.String)
				return cls != null ? Z8.create(cls, config) : new Z8.form.field.Text(config);

			if(type == Type.Geometry)
				return cls != null ? Z8.create(cls, config) : new Z8.form.field.Geometry(config);

			if(type == Type.Text) {
				config.minHeight = Ems.unitsToEms(field.height);
				return cls != null ? Z8.create(cls, config) : (field.html ? new Z8.form.field.Html(config) : new Z8.form.field.TextArea(config));
			}

			if(type == Type.Files) {
				config.height = Ems.unitsToEms(field.height || 3);
				return cls != null ? Z8.create(cls, config) : new Z8.form.field.Files(config);
			}

			if(type == Type.Guid)
				return cls != null ? Z8.create(cls, config) : new Z8.form.field.Text(config);

			if(type == Type.Datespan)
				return cls != null ? Z8.create(cls, config) : new Z8.form.field.Text(config);

			throw 'Unknown server type "' + type + '"';
		},

		createFieldset: function(fieldset) {
			var config = {
				name: fieldset.name,
				actions: fieldset.actions,
				colCount: fieldset.colCount,
				colSpan: fieldset.colSpan,
				readOnly: fieldset.readOnly,
				legend: fieldset.legend,
				icon: fieldset.icon,
				plain: !fieldset.isFieldset,
				height: Ems.unitsToEms(fieldset.height),
				flex: fieldset.flex
			};
			var cls = Application.getSubclass(fieldset.ui);
			return cls != null ? Z8.create(cls, config) : new Z8.form.Fieldset(config);
		},

		createTab: function(tab) {
			var config = {
				name: tab.name,
				title: tab.header,
				actions: tab.actions,
				colCount: tab.colCount,
				colSpan: tab.colSpan,
				readOnly: tab.readOnly,
				icon: tab.icon,
				height: Ems.unitsToEms(tab.height),
				flex: tab.flex
			};
			var cls = Application.getSubclass(tab.ui);
			return cls != null ? Z8.create(cls, config) : new Z8.form.Tab(config);
		}
	}
});