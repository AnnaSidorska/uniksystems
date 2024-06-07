package com.example.unisystems.model

open class User(
    val uid: String,
    val photoUrl: String?,
    val userTag: String,
    val surname: String,
    val name: String,
    val patronym: String,
    val serialNumber: String,
    val issueDate: String,
    val expDate: String,
    val faculty: String,
    val educationForm: String? = null,
    val groupName: String? = null,
    val position: String? = null)
{
    constructor() : this( "", "", "", "", "", "", "", "", "", "")
}
