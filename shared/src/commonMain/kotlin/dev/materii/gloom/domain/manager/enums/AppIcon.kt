package dev.materii.gloom.domain.manager.enums

import dev.icerock.moko.resources.ImageResource
import dev.icerock.moko.resources.StringResource
import dev.materii.gloom.Res

enum class AppIcon(
    val aliasName: String,
    val iconName: StringResource,
    val iconDescription: StringResource,
    val preview: ImageResource,
    val collection: AppIconCollection
) {

    // Classic

    Main(
        aliasName = "gloom.icons.classic.Main",
        iconName = Res.strings.app_icon_main,
        iconDescription = Res.strings.app_icon_main_description,
        preview = Res.images.gloom_icon_default,
        collection = AppIconCollection.Classic
    ),

    Sky(
        aliasName = "gloom.icons.classic.Sky",
        iconName = Res.strings.app_icon_sky,
        iconDescription = Res.strings.app_icon_sky_description,
        preview = Res.images.gloom_icon_sky,
        collection = AppIconCollection.Classic
    ),

    Light(
        aliasName = "gloom.icons.classic.Light",
        iconName = Res.strings.app_icon_light,
        iconDescription = Res.strings.app_icon_light_description,
        preview = Res.images.gloom_icon_light,
        collection = AppIconCollection.Classic
    ),

    // Stylized

    Blueprint(
        aliasName = "gloom.icons.stylized.Blueprint",
        iconName = Res.strings.app_icon_blueprint,
        iconDescription = Res.strings.app_icon_blueprint_description,
        preview = Res.images.gloom_icon_blueprint,
        collection = AppIconCollection.Stylized
    )

}

enum class AppIconCollection(
    val nameRes: StringResource
) {

    Classic(Res.strings.app_icon_collection_classic),
    Stylized(Res.strings.app_icon_collection_stylized)
}
