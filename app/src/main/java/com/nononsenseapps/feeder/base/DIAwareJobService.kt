package com.nononsenseapps.feeder.base

import android.app.job.JobService
import org.kodein.di.DI
import org.kodein.di.DIAware
import org.kodein.di.android.closestDI
import org.kodein.di.bind
import org.kodein.di.instance
import org.kodein.di.provider

abstract class DIAwareJobService :
    JobService(),
    DIAware {
    private val parentDI: DI by closestDI()
    override val di: DI by DI.lazy {
        extend(parentDI)
        // Use provider to avoid memory leak - holds factory function, not strong reference
        bind<DIAwareJobService>() with provider { this@DIAwareJobService }
    }
}
