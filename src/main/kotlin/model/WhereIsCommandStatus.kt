package model

sealed class WhereIsCommandStatus {
    object NoSuchDevice : WhereIsCommandStatus()
    object Free : WhereIsCommandStatus()
    class Taken(val owner: String) : WhereIsCommandStatus()
}