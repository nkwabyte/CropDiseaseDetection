require 'xcodeproj'
project_path = 'iosApp/iosApp.xcodeproj'
project = Xcodeproj::Project.open(project_path)
target = project.targets.first

# Add Assets.xcassets to the main group
main_group = project.main_group.find_subpath('iosApp', true)
file_ref = main_group.new_reference('Assets.xcassets')

# Add to Resources build phase
resources_phase = target.resources_build_phase
resources_phase.add_file_reference(file_ref)

project.save
