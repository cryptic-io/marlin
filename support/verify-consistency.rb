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
  all = marlins.map { |i| i.all.to_set }
  reduced = all.to_set
  if reduced.length != 1
    warn "#{$0}: inconsistent number of entries in instances"

    # group endpoints by number of entries in each
    lengths = {}
    all.each_with_index { |s, i|
      if !lengths.include?(s.length)
        lengths[s.length] = []
      end

      lengths[s.length].push(marlins[i].endpoint)
    }

    # print the number of entries and the corresponding endpoints
    lengths.each { |i|
      endpoints = i[1].join(', ')
      warn "#{$0}: #{i[0]} entries: #{endpoints}"
    }

    # when we're comparing only two endpoints, show the diff
    if all.length == 2
      largest, smallest = (all.first.length > all.last.length) \
                           ? [all.first, all.last] \
                           : [all.last, all.first]
      warn ("#{$0}: #{lengths[largest.length][0]} has additional entries: " \
            "#{largest.difference(smallest).to_a}")
    end
    return false
  end
  
  # compare the size and hash on all instances of each file
  return (reduced.first.map { |entry|
            if (marlins.map { |instance| instance.all(entry) }).to_set.length != 1
              warn "#{$0}: entry \"#{entry}\" inconsistent"
              next false
            end

            next true
          }).all?
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

endpoints = ARGV
if options[:file]
  endpoints += File.open(options[:file],'rb').read.split(/\n/)
end

if endpoints.empty?
  exit true
end

marlins = endpoints.map { |item|
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
