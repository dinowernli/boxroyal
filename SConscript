import os

def convert(word):
    return ''.join(x.capitalize() or '_' for x in word.split('_'))

root = os.path.abspath('..')
print 'Root:', root

boxroyal_package_dir = os.path.join('ch', 'nevill', 'boxroyal')

proto_dir = os.path.join(root, 'src', boxroyal_package_dir, 'proto')
build_dir = os.path.join(root, 'build')
lib_dir = os.path.join(root, 'lib')
src_dir = os.path.join(root, 'src')
bin_dir = os.path.join(root, 'build', 'bin')

environment = Environment(JAVACLASSPATH = lib_dir, JAVASOURCEPATH = [src_dir, build_dir])

# Build protos.
for proto_file in Glob(os.path.join(proto_dir, '*.proto')):
  basename = os.path.splitext(os.path.basename(str(proto_file)))[0]
  java_file = os.path.join(build_dir, boxroyal_package_dir, convert(basename) + '.java')
  print java_file
  environment.Command(
    java_file,
    str(proto_file),
    'protoc --java_out=%s --proto_path=%s %s' % (build_dir, proto_dir, str(proto_file)))

environment.Java(target='bin', source=[src_dir, build_dir])
