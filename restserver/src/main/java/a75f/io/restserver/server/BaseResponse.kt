/*
 * Created by nphau on 04/02/2022, 23:02
 * Copyright (c) 2022 . All rights reserved.
 * Last modified 04/02/2022, 23:02
 */

package a75f.io.restserver.server

data class BaseResponse<T>(val data: T? = null, val error: String? = null)