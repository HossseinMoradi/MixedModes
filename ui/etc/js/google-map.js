// This example adds a user-editable rectangle to the map.
// When the user changes the bounds of the rectangle,
// an info window pops up displaying the new bounds.

      var rectangle;
      var map;
      var infoWindow;

      function initMap() {
        map = new google.maps.Map(document.getElementById('map'), {
          center: {lat: 44.5452, lng: -78.5389},
          zoom: 9
        });

        var bounds = {
          north: 44.599,
          south: 44.490,
          east: -78.443,
          west: -78.649
        };

        // Define the rectangle and set its editable property to true.
        rectangle = new google.maps.Rectangle({
          bounds: bounds,
          editable: true,
          draggable: true
        });

        rectangle.setMap(map);

        // Add an event listener on the rectangle.
        rectangle.addListener('bounds_changed', showNewRect);

        // Define an info window on the map.
        infoWindow = new google.maps.InfoWindow();
      }
      // Show the new coordinates for the rectangle in an info window.

      /** @this {google.maps.Rectangle} */
      function showNewRect(event) {
        var ne = rectangle.getBounds().getNorthEast();
        var sw = rectangle.getBounds().getSouthWest();

        var contentString = '<b>Rectangle moved.</b><br>' +
            'New north-east corner: ' + ne.lat() + ', ' + ne.lng() + '<br>' +
            'New south-west corner: ' + sw.lat() + ', ' + sw.lng();

        // Set the info window's content and position.
        infoWindow.setContent(contentString);
        infoWindow.setPosition(ne);

        infoWindow.open(map);
      }

