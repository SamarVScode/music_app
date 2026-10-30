/*
 * Copyright (c) 2026 Auxio Project
 * SortMenu.kt is part of Auxio.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */
package org.oxycblt.auxio.ui.ios

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import org.oxycblt.auxio.home.HomeViewModel
import org.oxycblt.auxio.list.sort.Sort
import androidx.compose.material3.Switch
import androidx.compose.ui.Alignment

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SortMenu(
    onDismissRequest: () -> Unit,
    homeViewModel: HomeViewModel = hiltViewModel()
) {
    ModalBottomSheet(
        onDismissRequest = onDismissRequest,
        containerColor = Color.White,
        shape = RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp)
    ) {
        var isAscending by remember { mutableStateOf(true) }
        
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Sort By", fontWeight = FontWeight.Bold, fontSize = 20.sp)
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(if (isAscending) "Ascending" else "Descending", modifier = Modifier.padding(end = 8.dp))
                    Switch(checked = isAscending, onCheckedChange = { isAscending = it })
                }
            }
            Spacer(modifier = Modifier.height(16.dp))
            
            val options = listOf("Name", "Artist", "Album", "Duration", "Date Added")
            options.forEach { option ->
                Text(
                    text = option,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable {
                            val mode = when(option) {
                                "Name" -> Sort.Mode.ByName
                                "Artist" -> Sort.Mode.ByArtist
                                "Album" -> Sort.Mode.ByAlbum
                                "Duration" -> Sort.Mode.ByDuration
                                "Date Added" -> Sort.Mode.ByDateAdded
                                else -> Sort.Mode.ByName
                            }
                            val dir = if (isAscending) Sort.Direction.ASCENDING else Sort.Direction.DESCENDING
                            val sort = Sort(mode, dir)
                            homeViewModel.applySongSort(sort)
                            homeViewModel.applyAlbumSort(sort)
                            homeViewModel.applyArtistSort(sort)
                            onDismissRequest()
                        }
                        .padding(vertical = 12.dp),
                    fontSize = 16.sp
                )
            }
            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}
