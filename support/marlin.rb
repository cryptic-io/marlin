#!/usr/bin/ruby 

require 'net/http'

class Marlin
  def initialize(config)
    self.config = config
  end

  def config
    @config
  end

  def config=(val)
    @config = val
    rest = @config[:rest]
    @endpoint = "http://#{rest[:host]}:#{rest[:port]}"
  end

  def get_response(url)
    uri = URI(url)
    response = Net::HTTP.get_response(uri)
    if not response.is_a?(Net::HTTPSuccess)
      throw IOError.new("Expected HTTP 200 from \"#{url}\", " \
                        "but received HTTP #{response.code}")
    end

    return response.body
  end

  def all(filename=nil)
    if filename.nil?
      # list of files at /all
      get_response("#{@endpoint}/all").split("\n")
    else
      # nice Ruby hash containing the sha1 and size of filename
      Hash[get_response("#{@endpoint}/#{filename}/all")
             .split("\n")
             .collect { |i|
               key, value = i.split(' ')
               [key.to_sym, /^\d+$/ === value ? value.to_i : value]
             }]
    end
  end

  private :get_response

end
