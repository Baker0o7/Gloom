package dev.materii.gloom.ui.screen.home.component

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.PersonAddAlt
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.unit.dp
import dev.materii.gloom.gql.fragment.FollowRecommendationFeedItemFragment

@Composable
fun RecommendedFollowUserItem(
    item: FollowRecommendationFeedItemFragment,
    followData: Pair<Boolean, Int>?,
    onFollowClick: (String) -> Unit,
    onUnfollowClick: (String) -> Unit,
) {
    val id = item.followee.feedUser?.id ?: item.followee.feedOrg?.id ?: return
    Column(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        FeedActor(
            text        = AnnotatedString("Suggested for you"),
            iconVector  = Icons.Outlined.PersonAddAlt,
            iconDescription = "Suggested",
        )
        FeedUserCard(
            user            = Pair(item.followee.feedUser, item.followee.feedOrg),
            followData      = followData,
            onFollowClick   = { onFollowClick(id) },
            onUnfollowClick = { onUnfollowClick(id) },
        )
    }
}
