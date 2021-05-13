package com.example.habittracker.ui.fragments.HabitList

import android.widget.Filter
import android.widget.Filterable
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModel
import com.example.habittracker.HabitData.HabitRepository
import com.example.habittracker.habitClasses.Habit
import com.example.habittracker.habitClasses.HabitType
import kotlinx.coroutines.*
import kotlin.coroutines.CoroutineContext


class HabitListViewModel(private val habitType: HabitType) : ViewModel(), Filterable, CoroutineScope {


    private val mutableHabitList = MutableLiveData<List<Habit>>()
    val habits: LiveData<List<Habit>> = mutableHabitList
    private var repository = HabitRepository()
    private var notFilteredList = mutableHabitList.value

    private val job = SupervisorJob()
    override val coroutineContext: CoroutineContext
        get() = Dispatchers.Main + job + CoroutineExceptionHandler{_, e -> throw e}


    init {
        onCreate()
    }

    private lateinit var observer: Observer<List<Habit>>
    private fun onCreate() {
        observer = Observer {
            mutableHabitList.value = it.filter {  it.type == habitType }
            notFilteredList = mutableHabitList.value
        }
        HabitRepository.localHabits.observeForever(observer)
    }


    override fun getFilter(): Filter {
        return object : Filter() {
            override fun performFiltering(constraint: CharSequence?): FilterResults {
                val searchString = constraint.toString()
                val searchResult = FilterResults()
                if (searchString.isEmpty())
                    searchResult.values = notFilteredList
                else
                    searchResult.values = mutableHabitList.value!!.filter { it.name.contains(searchString) }
                return searchResult
            }

            override fun publishResults(constraint: CharSequence?, results: FilterResults?) {
                mutableHabitList.value = results?.values as? List<Habit>?
            }
        }
    }

    fun getHabits() = habits.value

    override fun onCleared() {
        super.onCleared()
        HabitRepository.localHabits.removeObserver(observer)
        coroutineContext.cancelChildren()
    }


    fun habitDeleted(habit: Habit) = launch {
        withContext(Dispatchers.IO) {
            repository.deleteHabit(habit)
        }
    }

    fun sortList(position: Int) = launch {
        withContext(Dispatchers.Default) {
            when (position) {
                0 -> mutableHabitList.postValue(mutableHabitList.value!!.sortedBy { it.id })
                1 -> mutableHabitList.postValue(mutableHabitList.value!!.sortedBy { it.name })
                2 -> mutableHabitList.postValue(mutableHabitList.value!!.sortedBy { it.priority.value })
            }
        }
    }

}
