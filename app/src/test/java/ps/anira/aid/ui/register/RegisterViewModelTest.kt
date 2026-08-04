package ps.anira.aid.ui.register

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import ps.anira.aid.data.AniraDatabase
import ps.anira.aid.data.Repository
import ps.anira.aid.ui.RelationOptions

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class RegisterViewModelTest {

    private val testDispatcher = StandardTestDispatcher()
    private lateinit var db: AniraDatabase
    private lateinit var repo: Repository
    private lateinit var vm: RegisterViewModel

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        db = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext(),
            AniraDatabase::class.java
        ).allowMainThreadQueries().build()
        repo = Repository(db.recordDao())
        vm = RegisterViewModel(repo)
    }

    @After
    fun tearDown() {
        db.close()
        Dispatchers.resetMain()
    }

    private fun fillValidForm() {
        vm.onBenNameChange("محمد هاني شلح")
        vm.onBenIdChange("405185299")
        vm.onDepNameChange("شمعه شلح")
        vm.onDepIdChange("205185299")
        vm.onRelationChange("بنت عم")
    }

    @Test
    fun `نداءان متتاليان سريعان لـ save ينتجان سجلاً واحداً فقط لا اثنين`() = runTest {
        fillValidForm()

        vm.save {}
        vm.save {}

        testDispatcher.scheduler.advanceUntilIdle()

        val all = db.recordDao().getAllIds()
        assertEquals("لازم سجل واحد بالضبط، مو اثنين", 1, all.size)
    }

    @Test
    fun `حقول ناقصة لا تُنشئ سجلاً وتُظهر رسالة خطأ`() = runTest {
        vm.onBenNameChange("اسم فقط بدون باقي الحقول")
        vm.save {}
        testDispatcher.scheduler.advanceUntilIdle()

        assertEquals(0, db.recordDao().getAllIds().size)
        // تم استبدال الأمر القديم بالأمر القياسي لاختبارات أندرويد
        assertNotNull("يجب أن تظهر رسالة خطأ عند نقص الحقول", vm.state.value.fieldError)
    }

    @Test
    fun `صلة أخرى بدون نص مخصص تُرفض`() = runTest {
        fillValidForm()
        vm.onRelationChange(RelationOptions.CUSTOM)
        vm.onCustomRelationChange("") // فاضي عمداً
        vm.save {}
        testDispatcher.scheduler.advanceUntilIdle()

        assertEquals(0, db.recordDao().getAllIds().size)
    }
}
