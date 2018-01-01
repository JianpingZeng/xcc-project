configure_lit_site_cfg(
  ${CMAKE_CURRENT_SOURCE_DIR}/lit.site.cfg.in
  ${CMAKE_CURRENT_BINARY_DIR}/lit.site.cfg
  )

# Set the depends list as a variable so that it can grow conditionally.
# NOTE: Sync the substitutions in test/lit.cfg when adding to this list.
set(XCC_TEST_DEPENDS
          llc
          opt
          jar
          jlang-cc
          jlang
        )

add_custom_target(xcc-test-depends DEPENDS ${XCC_TEST_DEPENDS})
set_target_properties(xcc-test-depends PROPERTIES FOLDER "Tests")


add_lit_testsuite(check-xcc "Running the XCC regression tests"
  ${CMAKE_CURRENT_BINARY_DIR}
  PARAMS xcc_site_config=${CMAKE_CURRENT_BINARY_DIR}/lit.site.cfg
  DEPENDS ${XCC_TEST_DEPENDS}
  )
set_target_properties(check-xcc PROPERTIES FOLDER "Tests")

# Setup a legacy alias for 'check-xcc'. This will likely change to be an
# alias for 'check-all' at some point in the future.
add_custom_target(check)
add_dependencies(check check-xcc)
set_target_properties(check PROPERTIES FOLDER "Tests")
