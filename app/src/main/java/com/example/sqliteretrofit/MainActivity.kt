package com.example.sqliteretrofit

import android.app.AlertDialog
import android.os.Bundle
import android.view.LayoutInflater
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import java.util.UUID

class MainActivity : AppCompatActivity() {
    private lateinit var listView: ListView
    private lateinit var swipe: SwipeRefreshLayout
    private lateinit var adapter: ArrayAdapter<String>
    private val userList = mutableListOf<String>()
    private val userMap = mutableListOf<User>()
    private lateinit var dbHelper: DatabaseHelper

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        dbHelper = DatabaseHelper(this)
        listView = findViewById(R.id.list)
        swipe = findViewById(R.id.swipe)
        adapter = ArrayAdapter(this, android.R.layout.simple_list_item_1, userList)
        listView.adapter = adapter

        swipe.setOnRefreshListener {
            syncFromServer()
        }

        findViewById<com.google.android.material.floatingactionbutton.FloatingActionButton>(R.id.fabAdd)
            .setOnClickListener { showUserForm(null) }

        listView.setOnItemClickListener { _, _, position, _ ->
            showUserForm(userMap[position])
        }

        listView.setOnItemLongClickListener { _, _, position, _ ->
            val user = userMap[position]
            AlertDialog.Builder(this)
                .setTitle("Hapus Data")
                .setMessage("Hapus ${user.name}?")
                .setPositiveButton("Hapus") { _, _ ->
                    user.id?.let {
                        dbHelper.deleteUser(it)
                        RetrofitClient.instance.deleteUser(it).enqueue(object : Callback<Void> {
                            override fun onResponse(call: Call<Void>, response: Response<Void>) {
                                runOnUiThread {
                                    fetchUsers()
                                }
                            }

                            override fun onFailure(call: Call<Void>, t: Throwable) {
                                t.printStackTrace()
                            }
                        })
                    }
                }
                .setNegativeButton("Batal", null)
                .show()
            true
        }

        fetchUsers()
    }

    private fun fetchUsers() {
        swipe.isRefreshing = true
        val users = dbHelper.getAllUsers()
        userList.clear()
        userMap.clear()
        users.forEach {
            userMap.add(it)
            userList.add("${it.name}, ${it.email}")
        }
        adapter.notifyDataSetChanged()
        swipe.isRefreshing = false
    }

    private fun showUserForm(user: User?) {
        val dialogView = LayoutInflater.from(this).inflate(R.layout.form, null)
        val editName = dialogView.findViewById<EditText>(R.id.editName)
        val editEmail = dialogView.findViewById<EditText>(R.id.editEmail)

        if (user != null) {
            editName.setText(user.name)
            editEmail.setText(user.email)
        }

        AlertDialog.Builder(this)
            .setTitle(if (user == null) "Tambah User" else "Edit User")
            .setView(dialogView)
            .setPositiveButton("Simpan") { _, _ ->
                val name = editName.text.toString()
                val email = editEmail.text.toString()
                if (user == null) {
                    val id = UUID.randomUUID().toString()
                    val newUser = User(id, name, email)
                    dbHelper.insertUser(newUser)
                    RetrofitClient.instance.insertUser(newUser).enqueue(object : Callback<User> {
                        override fun onResponse(call: Call<User>, response: Response<User>) {
                            runOnUiThread {
                                fetchUsers()
                            }
                        }

                        override fun onFailure(call: Call<User>, t: Throwable) {
                            t.printStackTrace()
                        }
                    })
                    fetchUsers()
                } else {
                    val updatedUser = User(user.id, name, email)
                    dbHelper.updateUser(updatedUser)
                    user.id?.let {
                        RetrofitClient.instance.updateUser(it, updatedUser).enqueue(object : Callback<Void> {
                            override fun onResponse(call: Call<Void>, response: Response<Void>) {
                                runOnUiThread {
                                    fetchUsers()
                                }
                            }

                            override fun onFailure(call: Call<Void>, t: Throwable) {
                                t.printStackTrace()
                            }
                        })
                    }
                    fetchUsers()
                }
            }
            .setNegativeButton("Batal", null)
            .show()
    }

    private fun syncFromServer() {
        swipe.isRefreshing = true

        RetrofitClient.instance.getUsers().enqueue(object : Callback<List<User>> {
            override fun onResponse(call: Call<List<User>>, response: Response<List<User>>) {
                if (response.isSuccessful) {
                    val serverUsers = response.body() ?: emptyList()

                    val localUsers = dbHelper.getAllUsers()

                    val serverIds = serverUsers.mapNotNull { it.id }

                    serverUsers.forEach { user ->
                        val existingUser = localUsers.find { it.id == user.id }

                        if (existingUser == null) {
                            dbHelper.insertUser(user)
                        } else {
                            dbHelper.updateUser(user)
                        }
                    }

                    // Sinkronisasi dari SQLite ke server (opsional, jika Anda ingin mengaktifkannya)
                    // localUsers.forEach { localUser ->
                    //    if (!serverIds.contains(localUser.id)) {
                    //        RetrofitClient.instance.insertUser(localUser)
                    //           .enqueue(object : Callback<User> {
                    //                override fun onResponse(call: Call<User>, response: Response<User>) {
                    //                    // Data berhasil dikirim ke server
                    //                }
                    //
                    //                override fun onFailure(call: Call<User>, t: Throwable) {
                    //                    t.printStackTrace()
                    //                }
                    //            })
                    //    }
                    // }

                    runOnUiThread {
                        fetchUsers()
                    }
                }
                swipe.isRefreshing = false // Pastikan ini ada di sini juga
            }

            override fun onFailure(call: Call<List<User>>, t: Throwable) {
                t.printStackTrace()
                swipe.isRefreshing = false // Pastikan ini ada di sini
            }
        })
    }
}