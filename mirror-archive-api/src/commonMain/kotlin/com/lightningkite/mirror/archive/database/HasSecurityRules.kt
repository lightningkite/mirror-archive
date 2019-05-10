package com.lightningkite.mirror.archive.database

/*

//REVERSED - default is disallow

allowRead(Condition.Always)
allowUpdate(Condition.Always)
allowInsertion { item -> }
fields.forEach {
    //Applies to gets (masking output, sorts, filters)
    permitRead(Condition.Always) //Using this field as a sort or filter adds this as a condition too

    //Specifically denotes the mask value
    mask(value)

    //Applies to updates
    permitUpdate(Condition.Always)

    //Applies to inserts and updates
    //Limits all operations to SET only
    tweak { value -> value }

    //Applies to updates
    ignore { value -> value == null }
}


PIPELINES
sort -> field permitted reads -> secure sort and additional condition
condition -> field permitted reads -> secure condition
item output -> field masks and permitted reads -> secure item output
item input -> field tweaks -> secure item input
operation -> field tweaks and ignores -> secure operation

PUBLICIZE SECURITY RULES
You can ask what the server can do for you, and find this information:
- This field is tweaked
- This field is ignored
- Updating this field is only permitted when X
- This field is only shown when X
*/
interface HasSecurityRules<T : Any> {
    val rules: SecurityRules<T>
}