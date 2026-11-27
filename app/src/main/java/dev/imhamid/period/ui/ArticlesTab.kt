package dev.imhamid.period.ui

import android.content.Intent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.Share
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.imhamid.period.data.Article

@Composable
private fun ArticleCard(a: Article, onClick: () -> Unit, modifier: Modifier = Modifier) {
    Row(
        modifier.clip(RoundedCornerShape(14.dp)).background(C.Surface).pressable { onClick() }.padding(horizontal = 14.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Txt(a.title, 15.sp, FontWeight.Medium, modifier = Modifier.weight(1f), maxLines = 3, lineHeight = 22.sp)
        Spacer(Modifier.width(8.dp))
        Txt(a.icon.ifBlank { "🩸" }, 40.sp, lineHeight = 46.sp)
    }
}

@Composable
fun ArticlesTab(vm: AppVM) {
    val cats = remember(vm.articles) { vm.articles.groupBy { it.category } }
    Column(Modifier.fillMaxSize().background(C.Paper).statusBarsPadding().verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Box(Modifier.fillMaxWidth().height(60.dp).padding(horizontal = 16.dp)) {
            Txt("مقاله‌ها", 22.sp, FontWeight.Bold, modifier = Modifier.align(Alignment.Center))
            Box(Modifier.align(Alignment.CenterEnd).size(44.dp).pressable(role = null) { vm.push(Screen.ArticleList(null)) }, contentAlignment = Alignment.Center) {
                Icon(Icons.Rounded.Search, "جستجو", tint = C.Text)
            }
        }
        if (vm.articles.isEmpty()) {
            Panel(Modifier.padding(horizontal = 16.dp)) {
                Txt("مقاله‌ای پیدا نشد. وصل بودن به اینترنت رو چک کن و مایگریشن ۰۰۶ رو اجرا کن.", 14.sp, color = C.Muted)
            }
        }
        cats.forEach { (cat, list) ->
            Column(Modifier.padding(horizontal = 16.dp)) {
                SectionHeader(cat, onMore = { vm.push(Screen.ArticleList(cat)) }, moreText = "همه")
            }
            Spacer(Modifier.height(4.dp))
            val pairs = list.chunked(2)
            LazyRow(contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 16.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                items(pairs) { pair ->
                    Column(Modifier.width(290.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        pair.forEach { a -> ArticleCard(a, { vm.push(Screen.Article(a.id)) }, Modifier.fillMaxWidth().height(86.dp)) }
                    }
                }
            }
            Spacer(Modifier.height(10.dp))
        }
        Spacer(Modifier.height(16.dp))
    }
}

@Composable
fun ArticleListScreen(vm: AppVM, category: String?) {
    var q by remember { mutableStateOf("") }
    val list = remember(vm.articles, category, q) {
        vm.articles.filter { (category == null || it.category == category) && (q.isBlank() || it.title.contains(q.trim()) || it.summary.contains(q.trim()) || it.body.contains(q.trim())) }
    }
    Page {
        TopBar(category ?: "جستجوی مقاله‌ها", onBack = { vm.pop() })
        Column(Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(horizontal = 16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            if (category == null) Field(q, { q = it }, "دنبال چی می‌گردی؟")
            if (list.isEmpty()) Txt("نتیجه‌ای پیدا نشد.", 15.sp, color = C.Muted, modifier = Modifier.padding(top = 20.dp))
            list.forEach { a -> ArticleCard(a, { vm.push(Screen.Article(a.id)) }, Modifier.fillMaxWidth()) }
            Spacer(Modifier.height(16.dp))
        }
    }
}

@Composable
fun ArticleScreen(vm: AppVM, id: String) {
    val a = vm.articles.firstOrNull { it.id == id }
    val ctx = LocalContext.current
    if (a == null) { vm.pop(); return }
    Page {
        TopBar("", onBack = { vm.pop() }, trailing = {
            Box(Modifier.size(44.dp).pressable(role = null) {
                val i = Intent(Intent.ACTION_SEND).apply {
                    type = "text/plain"
                    putExtra(Intent.EXTRA_SUBJECT, a.title)
                    putExtra(Intent.EXTRA_TEXT, "${a.title}\n\n${a.body}\n\n— لونا")
                }
                ctx.startActivity(Intent.createChooser(i, "اشتراک‌گذاری مقاله"))
            }, contentAlignment = Alignment.Center) { Icon(Icons.Rounded.Share, "اشتراک‌گذاری", tint = C.Text) }
        })
        Column(Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(horizontal = 20.dp)) {
            Box(Modifier.size(84.dp).clip(CircleShape).background(C.PinkSoft), contentAlignment = Alignment.Center) { Txt(a.icon.ifBlank { "🩸" }, 44.sp, lineHeight = 50.sp) }
            Spacer(Modifier.height(14.dp))
            Txt(a.category, 13.sp, FontWeight.Medium, C.Pink)
            Txt(a.title, 24.sp, FontWeight.Bold, lineHeight = 34.sp, modifier = Modifier.padding(top = 4.dp))
            Spacer(Modifier.height(14.dp))
            a.body.split("\n\n").forEach { para ->
                if (para.startsWith("## ")) Txt(para.removePrefix("## "), 18.sp, FontWeight.Bold, modifier = Modifier.padding(top = 10.dp, bottom = 4.dp))
                else Txt(para, 16.sp, lineHeight = 28.sp, modifier = Modifier.padding(bottom = 10.dp))
            }
            Txt("این مطلب جنبه‌ی آموزشی دارد و جای مشاوره‌ی پزشکی نیست.", 12.sp, color = C.Muted, modifier = Modifier.padding(vertical = 16.dp))
        }
    }
}
