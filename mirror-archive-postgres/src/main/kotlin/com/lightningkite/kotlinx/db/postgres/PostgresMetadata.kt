package com.lightningkite.kotlinx.db.postgres

object PostgresMetadata {
    object Columns {
        val column_name = "column_name"  // 	sql_identifier 	//Name of the column
        val ordinal_position = "ordinal_position"  // 	cardinal_number 	//Ordinal position of the column within the table (count starts at 1)
        val column_default = "column_default"  // 	character_data 	//Default expression of the column
        val is_nullable = "is_nullable"  // 	yes_or_no 	//YES if the column is possibly nullable, NO if it is known not nullable. A not-null constraint is one way a column can be known not nullable, but there can be others.
        val data_type = "data_type"  // 	character_data 	//Data type of the column, if it is a built-in type, or ARRAY if it is some array (in that case, see the view element_types), else USER-DEFINED (in that case, the type is identified in udt_name and associated columns). If the column is based on a domain, this column refers to the type underlying the domain (and the domain is identified in domain_name and associated columns).
        val character_maximum_length = "character_maximum_length"  // 	cardinal_number 	//If data_type identifies a character or bit string type, the declared maximum length; null for all other data types or if no maximum length was declared.
        val character_octet_length = "character_octet_length"  // 	cardinal_number 	//If data_type identifies a character type, the maximum possible length in octets (bytes) of a datum; null for all other data types. The maximum octet length depends on the declared character maximum length (see above) and the server encoding.
        val numeric_precision = "numeric_precision"  // 	cardinal_number 	//If data_type identifies a numeric type, this column contains the (declared or implicit) precision of the type for this column. The precision indicates the number of significant digits. It can be expressed in decimal (base 10) or binary (base 2) terms, as specified in the column numeric_precision_radix. For all other data types, this column is null.
        val numeric_precision_radix = "numeric_precision_radix"  // 	cardinal_number 	//If data_type identifies a numeric type, this column indicates in which base the values in the columns numeric_precision and numeric_scale are expressed. The value is either 2 or 10. For all other data types, this column is null.
        val numeric_scale = "numeric_scale"  // 	cardinal_number 	//If data_type identifies an exact numeric type, this column contains the (declared or implicit) scale of the type for this column. The scale indicates the number of significant digits to the right of the decimal point. It can be expressed in decimal (base 10) or binary (base 2) terms, as specified in the column numeric_precision_radix. For all other data types, this column is null.
        val datetime_precision = "datetime_precision"  // 	cardinal_number 	//If data_type identifies a date, time, timestamp, or interval type, this column contains the (declared or implicit) fractional seconds precision of the type for this column, that is, the number of decimal digits maintained following the decimal point in the seconds value. For all other data types, this column is null.
    }
    fun columns(table: String) = "SELECT * FROM information_schema.columns WHERE table_name = $table"
}