
class UserCtrl

    constructor: (@$log, @UserService) ->
        @$log.debug "constructing UserController"
        @users = []
        @getAllUsers()

    getAllUsers: () ->
        @$log.debug "getAllUsers()"

        @UserService.listUsers()
        .then(
            (data) =>
                @$log.debug "Promise returned #{data.length} Users"
                @users = data
            ,
            (error) =>
                @$log.error "Unable to get Users: #{error}"
            )


    deleteUser: (id) ->
      # route params must be same name as provided in routing url in app.coffee
      @$log.debug "deleteUser()"

      @UserService.deleteUser(id)
      .then(
        (data) =>
          console.log(data)
          @$log.debug "Promise returned #{data.length} Users"
          @users = this.getAllUsers()
      ,
        (error) =>
          @$log.error "Unable to get Users: #{error}"
      )

controllersModule.controller('UserCtrl', UserCtrl)