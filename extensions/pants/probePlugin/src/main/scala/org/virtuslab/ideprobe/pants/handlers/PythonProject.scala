package org.virtuslab.ideprobe.pants.handlers

import com.intellij.facet.FacetManager
import com.jetbrains.python.facet.PythonFacetSettings

import org.virtuslab.ideprobe.handlers.Modules
import org.virtuslab.ideprobe.handlers.Sdks
import org.virtuslab.ideprobe.pants.protocol.PythonFacet
import org.virtuslab.ideprobe.protocol.ModuleRef

object PythonProject {

  def facets(moduleRef: ModuleRef): Seq[PythonFacet] = {
    val module = Modules.resolve(moduleRef)
    val facets = FacetManager.getInstance(module).getAllFacets
    facets
      .map(f => f.getName -> f.getConfiguration)
      .collect { case (name, config: PythonFacetSettings) =>
        val sdk = Option(config.getSdk).map(Sdks.convert)
        PythonFacet(name, sdk)
      }
  }

}
