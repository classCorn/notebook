package anyname.com.notebook.models

import io.realm.RealmObject

open class MRealmItem(
        var items: String = ""
) : RealmObject()