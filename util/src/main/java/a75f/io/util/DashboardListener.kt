package a75f.io.util

/**
 * Created by Manjunath K on 07-02-2025.
 */

interface DashboardListener {
    fun onDashboardConfigured(isDashboardConfigured: Boolean)
}
interface DashboardRefreshListener {
    fun refreshDashboard(isDashboardConfigured: Boolean)
}