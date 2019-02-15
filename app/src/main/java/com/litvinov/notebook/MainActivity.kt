package com.litvinov.notebook

import android.animation.Animator
import android.content.Context
import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import android.os.Handler
import android.view.View
import android.view.animation.LinearInterpolator
import android.view.inputmethod.InputMethodManager
import android.widget.*
import com.litvinov.notebook.controllers.CRealmControllers
import io.realm.Realm
import io.realm.RealmConfiguration
import android.view.LayoutInflater
import android.view.animation.AccelerateInterpolator
import android.widget.LinearLayout
import android.widget.ScrollView
import android.graphics.Rect
import android.util.Log
import com.crashlytics.android.Crashlytics
import io.fabric.sdk.android.Fabric
import com.litvinov.notebook.controllers.GestureListener

class MainActivity : AppCompatActivity() {

    private lateinit var btnAdd: ImageButton
    private lateinit var editParentLayout: RelativeLayout
    private lateinit var editChildLayout: LinearLayout
    private lateinit var txtEdit: EditText
    private lateinit var scrollView: ScrollView
    private var editIndex = -1
    private var isHideControl = true

    override fun onBackPressed() {

        if (editParentLayout.visibility == View.VISIBLE) {
            showEditLayout(false)
        } else {
            super.onBackPressed()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        initRealm()
        initUI()
        initDynamicalList()
    }

    private fun initRealm() {
        Realm.init(baseContext)

        val config = RealmConfiguration.Builder()
                .name("db.realm")
                .schemaVersion(1L)
                .migration(CRealmControllers.RealmMigrations())
                .build()
        Realm.setDefaultConfiguration(config)
    }

    private fun initUI() {

        txtEdit = findViewById(R.id.txt_edit)

        btnAdd = findViewById(R.id.btn_main_add)
        btnAdd.setOnClickListener {

            this.forceCrash(it)

            txtEdit.setText("")
            showEditLayout(true)
        }

        val btnSave = findViewById<Button>(R.id.lay_edit_save)
        btnSave.setOnClickListener {
            val inputText = txtEdit.text.toString()

            if (inputText.isEmpty()) {
                showEditLayout(false)
                return@setOnClickListener
            }

            if (editIndex > -1) {
                listView.forEach { view ->
                    if (view.tag as Int == editIndex) {
                        val tv = view.findViewById<TextView>(R.id.item_text)
                        tv.text = inputText
                        list[editIndex] = inputText
                    }
                }
                updateRealm()
            } else {
                addItem(inputText)
            }
            updateRealm()
            showEditLayout(false)
        }

        val btnCancel = findViewById<Button>(R.id.lay_edit_cancel)
        btnCancel.setOnClickListener {
            showEditLayout(false)
        }

        editParentLayout = findViewById(R.id.lay_edit_parent)
        editParentLayout.visibility = View.GONE
        editParentLayout.setOnClickListener {
            showEditLayout(false)
        }
        val layEditControl = findViewById<RelativeLayout>(R.id.lay_edit_control)
        editParentLayout.viewTreeObserver.addOnGlobalLayoutListener {
            val screenHeight = editParentLayout.rootView.height
            val r = Rect()
            val view = window.decorView
            view.getWindowVisibleDisplayFrame(r)
            val h = (screenHeight - r.bottom) * (-1f)
            layEditControl.animate().translationY(h).setDuration(300L).setInterpolator(LinearInterpolator()).start()
        }

        editChildLayout = findViewById(R.id.lay_edit)
        showEditLayout(false)

        scrollView = findViewById(R.id.lay_scroll)

        //set PLUS_btn visibility when scroll MainListView
        scrollView.requestDisallowInterceptTouchEvent(true)
        scrollView.setOnTouchListener { _, _ ->
            isHideControl = true
            return@setOnTouchListener false
        }
        var oldY = 0
        scrollView.viewTreeObserver.addOnScrollChangedListener {
            if (isHideControl) {
                val y = scrollView.scrollY
                val dy = oldY - y
                oldY = y
                if (dy * dy > 25) {
                    showControls(dy > 0)
                }
            }
        }
    }

    private fun updateRealm() {
        CRealmControllers.setItems(list)
    }

    private fun showControls(show: Boolean) {
        val layControls = findViewById<LinearLayout>(R.id.lay_main_controls)
        val step = if (show) 0f else 200f
        layControls.animate().translationY(step).setInterpolator(LinearInterpolator()).start()
    }

    private fun showEditLayout(show: Boolean, index: Int = -1) {

        if (show) {
            editParentLayout.visibility = View.VISIBLE
        } else
            Handler().postDelayed({
                editParentLayout.visibility = View.GONE
            }, 300)

        val transition = if (show) 0f else 1200f
        editChildLayout.animate().translationY(transition).setDuration(200L).setInterpolator(LinearInterpolator()).start()
        val alpha = if (show) 1f else 0f
        editParentLayout.animate().alpha(alpha).setDuration(300L).setInterpolator(LinearInterpolator()).start()

        val imm = getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
        if (show) {
            txtEdit.requestFocus()
            imm.showSoftInput(txtEdit, InputMethodManager.SHOW_IMPLICIT)
        } else {
            val view = editParentLayout
            imm.hideSoftInputFromWindow(view.windowToken, 0)
        }

        editIndex = index
        if (index >= 0) {
            txtEdit.setText(list[index])
            txtEdit.setSelection(txtEdit.text.length)
        }
    }

    private var list = mutableListOf<String>()
    private var listView = mutableListOf<View>()

    private fun initDynamicalList() {
        list = CRealmControllers.getItems()
        listView = mutableListOf()

        list.forEach {
            addItem(it, false)
        }
    }

    private fun addItem(text: String, addToList: Boolean = true) {

        val itemTag = listView.size
        val parent = findViewById<LinearLayout>(R.id.lay_dynamical)
        val inflater = baseContext.getSystemService(Context.LAYOUT_INFLATER_SERVICE) as LayoutInflater
        val lay = inflater.inflate(R.layout.item_of_list, null)
        lay.tag = itemTag

        listView.add(lay)
        parent.addView(lay)

        val itemTextView = lay.findViewById<TextView>(R.id.item_text)
        itemTextView.text = text
        itemTextView.tag = itemTag
        val gestureMainLay = GestureListener(lay)
        gestureMainLay.setOnX {
            lay.translationX = it
        }
        gestureMainLay.setOnClick {
            Log.i("gesture.setOnClick", "${it.tag}")
            if (it.tag is Int) {
                showEditLayout(true, it.tag as Int)
            }
        }

        isHideControl = false
        scrollView.post {
            scrollView.fullScroll(ScrollView.FOCUS_DOWN)
        }

        lay.scaleY = 0f
        lay.animate().scaleY(1f).setInterpolator(AccelerateInterpolator()).setDuration(300).start()

        val remove = lay.findViewById<ImageButton>(R.id.item_remove)
        remove.tag = itemTag
        GestureListener(remove).setOnClick {
            val tag = it.tag as Int

            val listenerAnimator = object : Animator.AnimatorListener {
                override fun onAnimationRepeat(p0: Animator?) {}

                override fun onAnimationEnd(p0: Animator?) {
                    parent.removeView(listView[tag])
                    listView.removeAt(tag)
                    list.removeAt(tag)
                    listView.forEachIndexed { index, view ->
                        view.tag = index
                        view.findViewById<ImageButton>(R.id.item_remove).tag = index
                        val tv = view.findViewById<TextView>(R.id.item_text)
                        val t = tv.text.toString()
                        tv.text = t
                    }
                    updateRealm()
                }

                override fun onAnimationCancel(p0: Animator?) {}

                override fun onAnimationStart(p0: Animator?) {}

            }
            if (tag < listView.size) {
                listView[tag].pivotX = listView[tag].width - 29f
                listView[tag].pivotY = 29f
                listView[tag].animate()
                        .translationX(2000f)
                        .alpha(0.5f)
                        .setDuration(300)
                        .setListener(listenerAnimator)
                        .start()
            }
        }


        if (addToList) {
            list.add(text)
            updateRealm()
        }
    }

    fun forceCrash(view: View) {
        throw RuntimeException("This is a crash")
    }


}
