package dev.materii.gloom.ui.screen.home.component

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.BookmarkBorder
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.unit.dp
import dev.materii.gloom.gql.fragment.RecommendedRepositoryFeedItemFragment

@Composable
fun RecommendedRepoItem(
    item: RecommendedRepositoryFeedItemFragment,
    starData: Pair<Boolean, Int>?,
    onStarClick: (String) -> Unit,
    onUnstarClick: (String) -> Unit,
) {
    Column(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        FeedActor(
            text        = AnnotatedString("Recommended for you"),
            iconVector  = Icons.Outlined.BookmarkBorder,
            iconDescription = "Recommended",
        )
        FeedRepoCard(
            repo          = item.repository.feedRepository,
            starData      = starData,
            onStarClick   = onStarClick,
            onUnstarClick = onUnstarClick,
        )
    }
}
