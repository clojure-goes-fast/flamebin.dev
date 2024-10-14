(ns flamebin.init
  (:require malli.core
            malli.experimental.time
            malli.registry
            flamebin.util
            flamebin.config
            taoensso.timbre.tools.logging))

(malli.registry/set-default-registry!
 (malli.registry/composite-registry
  (malli.core/default-schemas)
  (malli.experimental.time/schemas)
  flamebin.util/nano-id-registry))

(taoensso.timbre.tools.logging/use-timbre)

(def dont-remove-this-namespace)

(flamebin.config/init-config)
