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
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import ps.anira.aid.data.AniraDatabase
import ps.anira.aid.data.Repository
import ps.anira.aid.ui.RelationOptions

/**
 * يثبت فعلياً — لا بمجرد قراءة الكود — أن قفل الضغط المزدوج (نفس إصلاح باگ
 * "نفس البيانات مسجّلة أكثر من مرة" الذي اكتُشف بالويب من لقطة شاشة حقيقية)
 * يمنع نداءين متتاليين سريعين لـ save() من إنشاء سجلين، بالضبط بنفس المبدأ:
 * فحص متزامن (isSaving) قبل أي نقطة تعليق (suspend) أولى.
 */
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

        // محاكاة ضغطتين سريعتين متتاليتين على نفس زر الحفظ، قبل ما تخلص أول واحدة
        vm.save {}
        vm.save {} // لازم تُتجاهل فوراً بسبب isSaving=true اللي انضبطت بشكل متزامن بالنداء الأول

        testDispatcher.scheduler.advanceUntilIdle() // خلّي كل الكوروتينات المعلَّقة تخلص

        val all = db.recordDao().getAllIds()
        assertEquals("لازم سجل واحد بالضبط، مو اثنين", 1, all.size)
    }

    @Test
    fun `حقول ناقصة لا تُنشئ سجلاً وتُظهر رسالة خطأ`() = runTest {
        vm.onBenNameChange("اسم فقط بدون باقي الحقول")
        vm.save {}
        testDispatcher.scheduler.advanceUntilIdle()

        assertEquals(0, db.recordDao().getAllIds().size)
        assert(vm.state.value.fieldError != null)
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
