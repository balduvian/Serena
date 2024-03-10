package org.balduvian

import io.ktor.client.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.utils.io.jvm.javaio.*

//https://www.weather.gov/source/hfo/xml/SurfState.xml

object Feed {
	data class Report(val date: String, val url: String, val discussion: String)

	//suspend fun get(url: String): String? {
	//	val feedURL = Settings.get<String>("feed_url")
//
//
	//	val client = HttpClient()
//
	//	//val response = client.request(feedURL)
//
	//	val channel = response.bodyAsChannel()
//
	//	val root = XML.parse(channel.toInputStream())
//
	//	val items = root.firstChildOfTag("rss")?.firstChildOfTag("channel")?.childrenOfTag("item") ?: return
//
	//	println(items.find { item ->
	//		item.firstChildOfTag("title")?.contents() == "Discussion"
	//	}?.firstChildOfTag("description")?.contents())
	//}
}
