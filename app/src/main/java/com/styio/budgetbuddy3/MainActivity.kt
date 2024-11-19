package com.styio.budgetbuddy3

import android.app.AlertDialog
import android.content.ContentValues
import android.database.Cursor
import android.database.sqlite.SQLiteDatabase
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.ProgressBar
import android.widget.TextView
import androidx.appcompat.app.AppCompatDelegate
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.NumberFormat
import java.time.LocalDate

class MainActivity : AppCompatActivity(), CategoryAdapter.OnItemClickListener {
    private lateinit var btnEditIncome: Button
    private lateinit var txtTotalIncomeTotal: TextView
    private lateinit var txtTotalIncomeSpent: TextView
    private lateinit var progIncome: ProgressBar
    private lateinit var txtRemainingFunds: TextView

    private lateinit var recyclerView: RecyclerView
    lateinit var categoryModelArrayList: ArrayList<CategoryModel>

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        supportActionBar?.hide()//hide the stupid default title
        AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO) //force light mode even on dark mode enabled devices.
        recyclerView= findViewById<RecyclerView>(R.id.recycle_card)
        categoryModelArrayList = ArrayList<CategoryModel>()

        Log.i("onCreate","Checking if Db has values..." )
        initializeDbTables() //check if its the same date that is stored + initialize arraylist for cards
        val categoryAdapter = CategoryAdapter(this, categoryModelArrayList, this)
        val linearLayoutManager = LinearLayoutManager(this, LinearLayoutManager.VERTICAL, false)
        recyclerView.layoutManager = linearLayoutManager
        recyclerView.adapter = categoryAdapter

        var btnInsertCategory = findViewById<Button>(R.id.new_category_button)
        btnInsertCategory.setOnClickListener(object: View.OnClickListener{
            override fun onClick(view: View?) {
                spawnCategoryDialog()
            }
        })
        txtTotalIncomeTotal = findViewById (R.id.total_income)
        txtTotalIncomeTotal.setOnClickListener(object : View.OnClickListener {
            override fun onClick(view: View?) {
                Log.i("onClick", "Income button clicked so id passed to spawnDialog will be -1")
                spawnDialogue(-1, null)
            }
        })
        txtTotalIncomeSpent = findViewById(R.id.income_spent)
        txtRemainingFunds = findViewById(R.id.remaining_funds)
        progIncome = findViewById(R.id.income_progress_bar)
//
        checkCurrentDate()
        //Log.i("onCreate","Refreshing display..." )
        calculateRemaining()
//        refreshDisplay(null)
    }
    override fun onItemClick(position: Int) {
        Log.i("onClickRecycler", "onclick called from button $position")
        spawnDialogue(position, true) //plus 2 bc db elements start 2
        //call spawn dialog here
    }
    //once the values have been go, then get the text from the textviews to calc progbar.
    fun initializeDbTables(){
        var dbHelper: MyDatabaseHelper = MyDatabaseHelper(this)
        val db = dbHelper.readableDatabase
        //check if date is initialized
        var cursor = db.rawQuery("SELECT COUNT(*) FROM current_date_table", null)
        var row_count = 0
        if (cursor.moveToFirst()) {
            row_count = cursor.getInt(0)
        }
        Log.i("initializeDTables","Row count for date table is $row_count" )
        if( row_count == 0){// insert date if it isnt already there
            val currentDate = LocalDate.now()
            val contentValues = ContentValues()
            contentValues.put("month",currentDate.month.value )
            contentValues.put("year",currentDate.year )
            Log.i("DB","Writing the current date to db" )
            writeToDatabase(contentValues, "current_date_table")
        }
        //do we have a total row?
        cursor = db.rawQuery("SELECT COUNT(*) FROM budget_table", null)
        row_count = 0
        if (cursor.moveToFirst()) {
            row_count = cursor.getInt(0)
        }
        Log.i("initializeDTables","Row count for budget table is $row_count" )
        if( row_count == 0){
            val contentValues = ContentValues()
            contentValues.put("category","overall")
            contentValues.put("total",0.0)
            contentValues.put("spent",0.0)
            contentValues.put("percent",0)
            writeToDatabase(contentValues, "budget_table")
        }
        //load values into adapter for recycler view (list of card layouts)
        Log.i("initDbTables", "err next line?")
        cursor = db.rawQuery("SELECT * FROM budget_table where _id > 1", null)//id 1 should always be total and anything greater will be in the recycle view.
        Log.i("initDbTables", "err prev  line?")
        while (cursor.moveToNext()) {
        Log.i("initDbTables", "err cursor  line?")
            val category = cursor.getString(cursor.getColumnIndexOrThrow("category")) //this is returning nulllllll!!!!!
            val spent = cursor.getDouble(cursor.getColumnIndexOrThrow("spent"))
            val total =  cursor.getDouble(cursor.getColumnIndexOrThrow("total"))
            val percent = cursor.getInt(cursor.getColumnIndexOrThrow("percent"))
            if (categoryModelArrayList != null){
                Log.i("dbtables", "this shit is not null")
            }
            Log.i("InitDbTablez", "values we got are $category, $spent, $total, $percent")
            val entry = CategoryModel(category, spent, total, percent)
            categoryModelArrayList.add(entry)
        Log.i("initDbTables", "err previous  line?")
        }
        Log.i("initDbTables", "err prev line2 ?")
        cursor.close()
            db.close()
        Log.i("initDbTables", "err prev line?3")

    }
    fun checkCurrentDate(){
        var dbHelper: MyDatabaseHelper = MyDatabaseHelper(this)
        val db = dbHelper.readableDatabase
        val currentDate = LocalDate.now()
        val cursor: Cursor = db.query("current_date_table", null, null, null, null, null, null)
        var savedYear: Int = 0
        var savedMonth: Int = 0
        while (cursor.moveToNext()) { //movetonext means if there is a row after the current one. initially, cursor is set to before the first row, so there will always be a movetonext
            savedYear = cursor.getInt(cursor.getColumnIndexOrThrow("year"))
            savedMonth = cursor.getInt(cursor.getColumnIndexOrThrow("month"))
            //Log.i("DB","Got $savedMonth and $savedYear from db" )
        }
        if (currentDate.year > savedYear || currentDate.month.value > savedMonth){//i.e. its jan of next year
            //resetView and and save current table to history, resetting db afterward.
            Log.i("DB","Looks like its a new month/year... reseting data" )
            //go row by row and insert values + date info into history table
            val cursor = db.rawQuery("SELECT * FROM budget_table", null)
            while (cursor.moveToNext()) {
                val row_total = cursor.getDouble(cursor.getColumnIndexOrThrow("total"))
                val row_id = cursor.getDouble(cursor.getColumnIndexOrThrow("_id"))
                val category = cursor.getString(cursor.getColumnIndexOrThrow("category"))
                //get totals here and pass them with their ids to resetView
                var values = ContentValues().apply {
                    // Append existing columns
                    put("spent", cursor.getDouble(cursor.getColumnIndexOrThrow("spent")))
                    put("total", row_total)
                    put("category", category) // come back to this. everything will have to be rebuilt to accomadate for this value
                    put("percent", cursor.getInt(cursor.getColumnIndexOrThrow("percent")))
                    put("year", savedYear)
                    put("month", savedMonth)
                }
                writeToDatabase(values,"history_table")
                var resetBudget = ContentValues().apply {
                   put("_id", row_id)
                   put("category", category)
                   put("spent", 0.0)
                   put("total", row_total)
                   put("percent", 0)
                }
                writeToDatabase(resetBudget, "budget_table")
            }
        }else{
            Log.i("DB","Still the same month... load data and display it!" )
        }
        db.close()
        refreshDisplay(null)
    }
    fun calculateRemaining(){
        Log.i("CalulateRemaining", "Calulating Remaining...")
    var dbHelper: MyDatabaseHelper = MyDatabaseHelper(this)
    val db = dbHelper.readableDatabase

    val cursor: Cursor = db.rawQuery( "select * from budget_table", null)
    var spent: Double = 0.0
    var total: Double = 0.0
    var percent: Int
        while (cursor.moveToNext()) { //movetonext means if there is a row after the current one. initially, cursor is set to before the first row, so there will always be a movetonext
        val id = cursor.getInt(cursor.getColumnIndexOrThrow("_id"))
        if (id == 1) {
            total = cursor.getDouble(cursor.getColumnIndexOrThrow("total")) //only initialize total budget
        }else{
            spent += cursor.getDouble(cursor.getColumnIndexOrThrow("spent")) //add total spent of all rows to overall spent.
        }
    }
    cursor.close()
        db.close()
    if (total != 0.0){
        percent = ((spent / total )* 100).toInt()
    }else{
        percent = 0
    }

    Log.i("CalulateRemaining", "Total spent should be $spent, and total should be $total with the percent being $percent%")
    val contentValues = ContentValues()
    contentValues.put("_id", 1)
    contentValues.put("category", "overall")
    contentValues.put("spent", spent)
    contentValues.put("percent", percent)
    contentValues.put("total", total)
    writeToDatabase(contentValues, "budget_table")

    refreshDisplay("1")//new spent should be written to db, so use refresh to query db and display it
        val remaining = total - spent
        txtRemainingFunds.text = NumberFormat.getCurrencyInstance().format(remaining).toString()
        Log.i("CalulateRemaining","Done - Updated remaining value should be displayed" )
    }
    fun spawnCategoryDialog(){
        val builder = AlertDialog.Builder(this)
        val inflater = layoutInflater
        val dialogView = inflater.inflate(R.layout.insert_modal, null)

        val categoryName = dialogView.findViewById<EditText>(R.id.category)
        val total = dialogView.findViewById<EditText>(R.id.total)

        builder.setView(dialogView)
            .setTitle("New Category")
            .setPositiveButton("OK") { dialog, _ ->
                val contentValues = ContentValues()
                //val category: String, var spent: Double, var total: Double, var percent: Int
                contentValues.put("category", categoryName.text.toString())
                contentValues.put("spent", 0.0)
                contentValues.put("total", total.text.toString().toDouble())
                contentValues.put("percent", 0)
                writeToDatabase(contentValues, "budget_table")
                refreshDisplay(null)
                dialog.dismiss()
            }
            .setNegativeButton("Cancel") { dialog, _ ->
                dialog.cancel()
            }
            .show()

    }
    fun spawnDialogue(id: Int, isPosition: Boolean?){ //null pointer exception?
        //get current value and display it in popup
        var currentTotal: Double = 0.0
        var currentSpent: Double = 0.0
        var category: String = ""

        if (id == -1){//income
            currentTotal = NumberFormat.getCurrencyInstance().parse(txtTotalIncomeTotal.text.toString()).toDouble()
            currentSpent = NumberFormat.getCurrencyInstance().parse(txtTotalIncomeSpent.text.toString()).toDouble()
        }
        else {
            currentTotal = categoryModelArrayList.get(id).total
            currentSpent = categoryModelArrayList.get(id).spent
            category = categoryModelArrayList.get(id).category

        }
        Log.i("spawndialog","Spawning popup..." )
        val builder = AlertDialog.Builder(this)
        val inflater = layoutInflater
        val dialogView = inflater.inflate(R.layout.modal_layout, null)

        val spent = dialogView.findViewById<EditText>(R.id.spent)
        val total = dialogView.findViewById<EditText>(R.id.total)
        total.hint = NumberFormat.getCurrencyInstance().format(currentTotal)

        var spent_submitted: Double
        var total_submitted: Double

        builder.setView(dialogView)
            .setTitle("Input That Sweet Info")
            .setPositiveButton("OK") { dialog, _ ->
                val spent_input = spent.text.toString()
                val total_input = total.text.toString()
                val contentValues = ContentValues()
                if(spent_input != "") {
                    spent_submitted = currentSpent + spent_input.toDouble()//add the new transaction to the overall total
                }else {
                    spent_submitted = currentSpent.toDouble()
                }
                if(total_input != "") {
                    total_submitted = total_input.toDouble()
                    Log.i("SpawnDialog", "Total Input updated to $total_input")
                }else {
                   total_submitted = currentTotal
                }
//                if (isPosition == true){
//
//                contentValues.put("_id", id +2)
//                }else{
//                    contentValues.put("_id", id + 2)
//                }
                contentValues.put("_id", id + 2)
                //
                contentValues.put("spent", spent_submitted)
                contentValues.put("total", total_submitted)
                contentValues.put("category", category)
                if(total_submitted != 0.0){
                contentValues.put("percent", (spent_submitted / total_submitted *100).toInt())
                }else{
                    contentValues.put("percent", 0)
                }
                //write change to db
                Log.i("SpawnDialog","Writing New values $contentValues" ) //where category??????
                writeToDatabase(contentValues, "budget_table")
                //write to overall total
                calculateRemaining()//this already calls refresh display for id 1 so dont call if its 1.
                //update view..
                if( id != -1) {
                    Log.i("SpawnDialog", "Refreshing display with id $id")
                    refreshDisplay((id + 2).toString()) //this might need to change?
                }
                dialog.dismiss()
            }
            .setNegativeButton("Cancel") { dialog, _ ->
                dialog.cancel()
            }
            .show()
    }
    fun refreshDisplay(targetId: String?){
        Log.i("RefreshDisplay", "Our id to refresh is $targetId")
        var dbHelper: MyDatabaseHelper = MyDatabaseHelper(this)
        lifecycleScope.launch {
            val cursor = withContext(Dispatchers.IO) {
                if (targetId != null){
                    Log.i("RefreshD", "Id is not null!")
                    dbHelper.readableDatabase.query("budget_table", null, "_id= ?", arrayOf(targetId), null, null, null)
                }else{
                    dbHelper.readableDatabase.query("budget_table", null, null, null, null, null, null)
                }
            }
                while (cursor.moveToNext()) {
                    val id = cursor.getInt(cursor.getColumnIndexOrThrow("_id"))
                    val category = cursor.getString(cursor.getColumnIndexOrThrow("category"))
                    val total = cursor.getDouble(cursor.getColumnIndexOrThrow("total"))
                    val spent = cursor.getDouble(cursor.getColumnIndexOrThrow("spent"))
                    val percent = cursor.getInt(cursor.getColumnIndexOrThrow("percent"))
                    Log.i("RefreshDisplay", "Values we are refreshing are: $id, $category, $total,$spent,$percent")

                    if(id == 1){ //if its overall category
                        txtTotalIncomeSpent.text = NumberFormat.getCurrencyInstance().format(spent).toString()
                        txtTotalIncomeTotal.text = NumberFormat.getCurrencyInstance().format(total).toString()
                        progIncome.progress = percent
                    }else{ // id 2 would be position 0 in arraylist and so on...
                        val position = id - 2
                    Log.i("RefreshDisplay", "position ends up being $position")
                    Log.i("RefreshDisplay", "array list is length $categoryModelArrayList.size")
                        if (position >= categoryModelArrayList.size){
                            categoryModelArrayList.add(CategoryModel(category, spent, total, percent))
                        }else {
                            categoryModelArrayList[position].spent = spent
                            categoryModelArrayList[position].total = total
                            categoryModelArrayList[position].category = category
                            categoryModelArrayList[position].percent =
                                ((spent / total) * 100).toInt()
                        }
                        Log.i("refreshD", "error next line?")
                        recyclerView.adapter!!.notifyDataSetChanged() //!! means non null asserted - i think its a way to tell the code its def not gon be null
                        //if position is greater than or equal to length, insert a new entry into array list
                    }

            }
            cursor.close()
            dbHelper.close()
        }
        Log.i("DB","Display refresh done" )
    }
    fun  writeToDatabase(contentValues: ContentValues, table: String){
        var dbHelper: MyDatabaseHelper = MyDatabaseHelper(this)
        val db = dbHelper.writableDatabase
        db.insertWithOnConflict(table, null, contentValues, SQLiteDatabase.CONFLICT_REPLACE)
        db.close()
        Log.i("DB_WriteToDatabase", "values $contentValues written to $table")
//        Toast.makeText(this, "Values Inserted!", Toast.LENGTH_SHORT).show()
    }

}//class