#!/usr/bin/ruby

require 'rubygems'
require 'bundler/setup'
require 'edn'

require 'set'
require 'optparse'
require 'net/http'

require_relative 'marlin'

def verify_consistency(marlins)
  # Create a list of sets of the entries from all.
  # When we convert this list to a set, it should have one element,
  # since all the elements of the list should be the same (as sets).
  # If not, then we can use the list of sets to give some more info
  # to aid in debugging (which instances have what lengths).
  all = marlins.map({ |x| x.all.to_set })
  reduced = all.to_set
  if reduced.length != 1
    warn "#{$0}: inconsistent number of entries in instances"

    # Group endpoints by number of entries in each
    lengths = {}
    all.each_with_index { |s, i|
      if !lengths.include?(s.length)
        lengths[s.length] = []
      end

      lengths[s.length].push(marlins[i].endpoint)
    }

    lengths.each { |i|
      endpoints = i[1].join(', ')
      warn"#{$0}: #{i[0]} entries: #{endpoints}"
    }

    # When we're comparing only two endpoints, show the diff
    if all.length == 2
      first, last = all.first, all.last
      biggest = first <= last ? last : first
      smallest = first == biggest ? last : first
      diff = biggest.difference(smallest).to_a.join(', ')
      warn "#{$0}: extra entries: #{diff}"
    end

    return false
  end
  
  
  # compare the size and hash on all instances of each file
end

# Parse the command-line options and act accordingly
options = {}
OptionParser.new do |o|
  o.banner += ' [config endpoint ...]'

  o.on('-f', '--file LIST', 'a file with a list of endpoints') do |f|
    if !File.exists?(f)
      abort "#{$0}: could not find file \"#{a}\""
    end

    options[:file] = f
  end

  o.on('-h', '--help', 'print this usage message and exit') do
    puts o
    exit
  end

  o.parse!

end

marlins = ARGV.map { |item|
  if item.upcase.start_with?('HTTP')
    uri = URI(item)
    config = {
      :rest => {
        :host => uri.hostname,
        :port => uri.port
      }
    }

    Marlin.new(config)
  else
    Marlin.new(EDN.read(File.open(item, 'rb').read))
  end
}

exit verify_consistency(marlins)
