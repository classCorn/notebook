package com.litvinov.notebook.controllers

import com.litvinov.notebook.models.MRealmItem
import io.realm.DynamicRealm
import io.realm.Realm
import io.realm.RealmMigration

class CRealmControllers {

    companion object {

        fun getItems(): MutableList<String> {

            var result = mutableListOf<String>()
            val realm = Realm.getDefaultInstance()
            val r = realm.where(MRealmItem::class.java).findFirst()

            r?.items?.let {
                val list = it.split("%]".toRegex())
                result = list.toMutableList()
            }

            return result
        }

        fun setItems(items: MutableList<String>) {
            val realm = Realm.getDefaultInstance()
            val mRealmItem = realm.where(MRealmItem::class.java).findFirst()

            var str = ""
            items.forEachIndexed { i, s ->
                str += if (i < (items.size - 1))
                    "$s%]"
                else s
            }

            if (mRealmItem != null) {
                realm.beginTransaction()
                mRealmItem.items = str
                realm.commitTransaction()
            } else {
                realm.executeTransaction {
                    val mRealmItem1 = realm.createObject(MRealmItem::class.java)
                    mRealmItem1.items = str
                }
            }

            realm.close()
        }

    }

    class RealmMigrations : RealmMigration {

        override fun hashCode(): Int {
            return RealmMigration::class.java.hashCode()
        }

        override fun equals(other: Any?): Boolean {
            if (other == null)
                return false
            return other is RealmMigrations
        }

        override fun migrate(realm: DynamicRealm?, oldVersion: Long, newVersion: Long) {
            //val schema = realm?.schema
        }

    }

}