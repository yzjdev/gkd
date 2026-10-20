package li.gkd.app.network

import li.gkd.app.R

// Real requests determine reachability; no unrelated host gates them.
fun isNetworkRequestAllowed(): Boolean = true
