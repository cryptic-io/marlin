#!/usr/bin/ruby

require 'optparse'
require 'net/http'
require 'digest'

require_relative 'marlin'

# Verify the integrity of a Marlin instance. An error consists of
# 
# * the Marlin entry not being on disk at the expected location
# * the file being present on disk but unable to be read by the 
#   current process
# * the file's size does not match the size stored in redis
# * the file's SHA-1 hash does not match the hash stored in redis
#
# Returns true if any error occurs and false otherwise. Errors
# messages are written to stderr via warn.
def verify_integrity(marlin)
  marlin.all().map { |entry|
    path = File.join(marlin.config[:root], *(entry.chars.take(3)), entry)
    if !File.exists?(path)
      warn "#{$0}: \"#{entry}\" does not exist at \"#{path}\""
      next true
    end

    if !File.readable?(path)
      warn "#{$0}: \"#{entry}\" cannot be read at \"#{path}\""
      next true
    end
    
    info = marlin.all(URI.encode(entry))
    size = File.size(path)
    if size != info[:size]
      warn "#{$0}: expected size of \"#{entry}\" (#{info[:size]})"\
           " does not match actual size of \"#{path}\" (#{size})"
      next true
    end

    digest = Digest::SHA1.file(path).hexdigest
    if digest != info[:hash]
      warn "#{$0}: expected hash of \"#{entry}\" (\"#{info[:hash]}\")"\
           " does not match actual hash of \"#{path}\" (#{digest})"
      next true
    end

    false
  }
end

# Parse the command-line options and act accordingly
options = {}
OptionParser.new do |o|
  o.banner += ' [config1 config2 ...]'

  o.on('-e', '--endpoint ADDRESS', 'specify the marlin HTTP endpoint') do |a|
    uri = URI(a)
    if uri.scheme.nil? || !["HTTP", "HTTPS"].include?(uri.scheme.upcase)
      abort "#{$0}: endpoint must be http or https"
    end

    options[:endpoint] = uri
  end

  o.on('-r', '--root DIRECTORY', "specify marlin's root directory") do |d|
    if !File.directory?(d)
      abort "#{$0}: marlin's root directory must actually be a directory"
    end

    options[:root] = d
  end

  o.on('-h', '--help', 'print this usage message and exit') do
    puts o
    exit
  end

  o.parse!

  # if one is set, the other must be as well
  either = !(options[:endpoint].nil? && options[:root].nil?)
  both = !(options[:endpoint].nil? || options[:root].nil?)
  if either && !both
    abort "#{$0}: must specify endpoint and root directory" 
  end

  if !either && ARGV.empty?
    puts o
    exit
  end
end

if !options.empty?
  endpoint = options[:endpoint]
  config = {
    :rest => {
      :host => endpoint.hostname,
      :port => endpoint.port
    },

    :root => options[:root]
  }

  verify_integrity(Marlin.new(config))
end
