package com.sparkdev.foodapp.models.firebase;

import android.content.Context;
import android.support.annotation.NonNull;
import android.support.v7.widget.RecyclerView;
import android.util.Log;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.FirebaseApp;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.EventListener;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.FirebaseFirestoreException;
import com.google.firebase.firestore.FirebaseFirestoreSettings;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.QuerySnapshot;
import com.sparkdev.foodapp.mainscreen.FoodMenu.FoodMenuAdapter;
import com.sparkdev.foodapp.mainscreen.productpage.ReviewsAdapter;
import com.sparkdev.foodapp.models.Order;
import com.sparkdev.foodapp.models.OrderItem;
import com.sparkdev.foodapp.models.OrdersCollection;
import com.sparkdev.foodapp.models.ReviewsCollection;
import com.sparkdev.foodapp.models.SingleMenuItem;
import com.sparkdev.foodapp.models.MenuCategory;
import com.sparkdev.foodapp.models.Review;
import com.sparkdev.foodapp.models.User;
import com.sparkdev.foodapp.models.firebase.foodMenuInterface.GetCategoryMenuItemsCompletionListener;
import com.sparkdev.foodapp.models.firebase.foodMenuInterface.GetMenuCategoriesCompletionListener;
import com.sparkdev.foodapp.models.firebase.loginInterface.GetUserCompletionListener;
import com.sparkdev.foodapp.models.firebase.loginInterface.LoginCompletionListener;
import com.sparkdev.foodapp.models.firebase.signupInterface.SignUpCompletionListener;
import com.sparkdev.foodapp.registerscreen.RegisterActivity;

import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

import javax.annotation.Nullable;

public class FirebaseAdapter {

  // refer to a single instance of this class
  private static FirebaseAdapter sFirebaseAdapter;

  // constant to use as a tag for console log messages
  private static final String TAG = "FIREBASE";

  private FirebaseFirestore mFirestore;

  private FirebaseAdapter(Context context) {
    // initialize the default FirebaseApp instance
    FirebaseApp.initializeApp(context);
    Log.d(TAG, "Firebase Firestore has been initialized");
    // get the default FirebaseDatabase instance
    mFirestore = FirebaseFirestore.getInstance();
    FirebaseFirestoreSettings settings = new FirebaseFirestoreSettings.Builder()
            .setTimestampsInSnapshotsEnabled(true)
            .build();
    mFirestore.setFirestoreSettings(settings);
  }

  public static FirebaseAdapter getInstance(Context context) {

    if (sFirebaseAdapter != null) {
      return sFirebaseAdapter;
    } else {
      sFirebaseAdapter = new FirebaseAdapter(context);
      return sFirebaseAdapter;
    }
  }

  public void loginUser(String email, String password, final LoginCompletionListener listener) {

    FirebaseAuth auth = FirebaseAuth.getInstance();

    auth.signInWithEmailAndPassword(email, password).addOnCompleteListener(new OnCompleteListener<AuthResult>() {
      @Override
      public void onComplete(@NonNull Task<AuthResult> task) {

        if (task.isSuccessful()) {
          AuthResult login = task.getResult();
          String uid = login.getUser().getUid();

          User.setCurrentUID(uid);

          getUserWithUID(uid, new GetUserCompletionListener() {
            @Override
            public void onSuccess(User user) {
              listener.onSuccess();
            }

            @Override
            public void onFailure() {
              listener.onFailure();
            }
          });

        } else {
          listener.onFailure();
        }

      }
    });

  }

  public void registerUser(final String email, String password, final SignUpCompletionListener listener) {

    // create an instance of the FirebaseAuth class
    FirebaseAuth auth = FirebaseAuth.getInstance();
    // call the createUserWithEmailAndPassword from the FirebaseAuth class and add a
    // listener to execute the argument after the user is registered in the Firebase Authentication
    auth.createUserWithEmailAndPassword(email, password).addOnCompleteListener(
            new OnCompleteListener<AuthResult>() {
              @Override
              public void onComplete(@NonNull Task<AuthResult> task) {
                if (task.isSuccessful()) {
                  // the user was successfully created in the Firebase Authentication area, BUT NOT in the
                  // Firestore database so get the created user's random generated ID
                  String userId = task.getResult().getUser().getUid();

                  // set an local instance of the current user by using their Firebase ID
                  User.setCurrentUID(userId);

                  // create the User object instance, refer to method getUserWithUID()
                  getUserWithUID(userId, new GetUserCompletionListener() {
                    @Override
                    public void onSuccess(User user) {

                      User.setCurrentUser(user);

                    }

                    @Override
                    public void onFailure() {

                      // do nothing

                    }
                  });


                  // create a user document in the Users collection in Firestore
                  CollectionReference usersRef = mFirestore.collection("Users");
                  DocumentReference usersDocRef = usersRef.document(userId);

                  // create a HashMap data structure that will later be turned into a JSON object using
                  // Firebase's set() method
                  HashMap<String, Object> user = new HashMap<>();
                  user.put("email", email);
                  user.put("firstName", "");  // create a first name field, user can change it later
                  user.put("lastName", "");   // create a last name field, user can change it later
                  user.put("address", "");    // create an address field, user can change it later


                  // create the document with the above HashMap
                  usersDocRef.set(user);

                  // registration was successful, implement listener in activity to do what it needs to
                  // do when a user is registered successfully
                  listener.onSuccess();

                } else {
                  // if registration was unsuccessful, implement listener in activity to do what it needs
                  // to do when a user is NOT registered successfully
                  listener.onFailure();
                }
              }
            });

  }

  public void getUserWithUID(String userID, final GetUserCompletionListener userCompletionListener) {
    // have a reference to the Users collection
    CollectionReference userRef = mFirestore.collection("Users");
    final DocumentReference userDoc = userRef.document(userID);

    userDoc.get().addOnCompleteListener(new OnCompleteListener<DocumentSnapshot>() {
      @Override
      public void onComplete(@NonNull Task<DocumentSnapshot> task) {

        if (task.isSuccessful()) {

          // get User from the Firebase Firestore base and convert it into a User java object
          User userObj = task.getResult().toObject(User.class);

          // if registration was successful, implement listener in registerUser() to do what it
          // needs to do when a user is NOT registered successfully

          userCompletionListener.onSuccess(userObj);

          // set the current User object instance
          User.setCurrentUser(userObj);

          // Log the current success
          Log.d(TAG, "Current user is: " + userObj.getEmail());

        } else {

          // if registration was unsuccessful, implement listener in registerUser() to do what it
          // needs to do when a user is NOT registered successfully

          userCompletionListener.onFailure();

          // Log the error message
          Log.d(TAG, task.getException().getMessage());

        }
      }
    });
  }

  public void getMenuCategories(final GetMenuCategoriesCompletionListener listener) {

    CollectionReference categoriesRef = mFirestore.collection("Categories");

    categoriesRef.get().addOnCompleteListener(new OnCompleteListener<QuerySnapshot>() {
      @Override
      public void onComplete(@NonNull Task<QuerySnapshot> task) {

        if (task.isSuccessful()) {

          List<MenuCategory> categories = new ArrayList<>();

          List<DocumentSnapshot> docs = task.getResult().getDocuments();

          for (DocumentSnapshot doc : docs) {

            MenuCategory menuCategory = doc.toObject(MenuCategory.class);
            categories.add(menuCategory);
          }

          listener.onSuccess(categories);

        } else {

          listener.onFailure();

        }

      }
    });

  }

  public void getMenuItems(MenuCategory menuCategory,
                           final GetCategoryMenuItemsCompletionListener listener) {

    CollectionReference menuItemsRef = mFirestore.collection("Foods");

    if (menuCategory.getCategoryId().matches("All")) {

      Task<QuerySnapshot> allMenuItems = menuItemsRef
              .get().addOnCompleteListener(new OnCompleteListener<QuerySnapshot>() {
                @Override
                public void onComplete(@NonNull Task<QuerySnapshot> task) {

                  if (task.isSuccessful()) {
                    List<SingleMenuItem> menuItems = new ArrayList<>();

                    for (QueryDocumentSnapshot document : task.getResult()) {
                      menuItems.add(document.toObject(SingleMenuItem.class));
                    }

                    listener.onSuccess(menuItems);

                  } else {
                    listener.onFailure();
                  }
                }
              });

    } else {
      Task<QuerySnapshot> allMenuItems = menuItemsRef
              .whereArrayContains("category", menuCategory.getCategoryId())
              .get().addOnCompleteListener(new OnCompleteListener<QuerySnapshot>() {
                @Override
                public void onComplete(@NonNull Task<QuerySnapshot> task) {

                  if (task.isSuccessful()) {
                    List<SingleMenuItem> menuItems = new ArrayList<>();

                    for (QueryDocumentSnapshot document : task.getResult()) {
                      menuItems.add(document.toObject(SingleMenuItem.class));
                    }

                    if(!menuItems.isEmpty())
                        listener.onSuccess(menuItems);
                    else
                        listener.onFailure();

                  } else {
                    listener.onFailure();
                  }
                }
              });
    }
  }

  public void submitReview(final SingleMenuItem menuItem, final Review newReview,
                           final UpdateMenuItemReviewsCompletionListener listener) {

    // Update the menu item's rating
    final DocumentReference menuItemRef =
            mFirestore.collection("Foods").document(menuItem.getId());

    menuItemRef.update("latestReview", newReview.convertToMap());


    final DocumentReference reviewsRef =
            mFirestore.collection("Reviews").document(menuItem.getReviewsRefId());

    reviewsRef.get().addOnCompleteListener(new OnCompleteListener<DocumentSnapshot>() {
      @Override
      public void onComplete(@NonNull Task<DocumentSnapshot> task) {

        if (task.isSuccessful()) {

          double sum = 0;
          double newRating = 0;

          ReviewsCollection currReviews = task.getResult().toObject(ReviewsCollection.class);
          currReviews.addReview(newReview);

          for (Review review : currReviews.getReviews()) {
            sum += review.getRating();
          }

          newRating = sum / currReviews.getReviews().size();

          menuItemRef.update("rating", newRating);

          reviewsRef.update("reviews", currReviews.convertToMap());

          listener.onSuccess();

        } else {

          listener.onFailure();
        }

      }
    });

  }

  public void updateProfile() {

    //get current user
    User user = User.currentUser;

    //Get a document reference to the current User
    final DocumentReference usersRef =
            mFirestore.collection("Users").document(User.currentUID);

    //update user fields
    usersRef.update("firstName", user.getFirstName());
    usersRef.update("lastName", user.getLastName());
    usersRef.update("email", user.getEmail());
    usersRef.update("address", user.getAddress());

  }



  public void menuItemsListener(final SingleMenuItem menuItem) {

    DocumentReference menuItemRef =
            mFirestore.collection("MenuItems").document(menuItem.getId());


    menuItemRef.addSnapshotListener(new EventListener<DocumentSnapshot>() {
      @Override
      public void onEvent(@Nullable DocumentSnapshot documentSnapshot, @Nullable FirebaseFirestoreException e) {

        if (e != null) {
          Log.w(TAG, "Listener error", e);
        }

        Log.d(TAG,
                "Menu item: " + menuItem.getName() + " reviews updated: " + documentSnapshot.getData());

      }
    });

  }


  public void reviewsListener(final SingleMenuItem menuItem, final ReviewsAdapter adapter, final RecyclerView recyclerView) {

    DocumentReference menuItemReviewsRef =
            mFirestore.collection("Reviews").document(menuItem.getReviewsRefId());


    menuItemReviewsRef.addSnapshotListener(new EventListener<DocumentSnapshot>() {
      @Override
      public void onEvent(@Nullable DocumentSnapshot documentSnapshot, @Nullable FirebaseFirestoreException e) {

        if (e != null) {
          Log.w(TAG, "Listener error", e);
        }

        ReviewsCollection newReviews = documentSnapshot.toObject(ReviewsCollection.class);
        adapter.chanegList(newReviews.getReviews());
        adapter.notifyDataSetChanged();
        recyclerView.smoothScrollToPosition(adapter.getItemCount());

        Log.d(TAG, "Reviews updated: " + documentSnapshot.getData());


      }

    });

  }


  public void newOrder(final List<OrderItem> orderItems, final NewOrderCompletionListener listener) {

    //if the user doesnt have orderRefId, create a new orders document and get the id
    if(User.currentUser.getOrdersRef() == null) {
        addOrderDocForUser(new CreateNewOrderDocumentCompletionListener() {
          @Override
          public void onSuccess() {
            Log.d(TAG, "new order document created with id: " + User.currentUser.getOrdersRef());

            //call newOrder again
            newOrder(orderItems, new NewOrderCompletionListener() {
              @Override
              public void onSuccess() {
                listener.onSuccess();
              }

              @Override
              public void onFailure() {
                listener.onFailure();
              }
            });

          }

          @Override
          public void onFailure() {
            listener.onFailure();
          }
        });
    }
    else{
      final String userOrderRef = User.currentUser.getOrdersRef();

      final DocumentReference ordersRef =
              mFirestore.collection("Orders").document(userOrderRef);

      ordersRef.get().addOnCompleteListener(new OnCompleteListener<DocumentSnapshot>() {
        @Override
        public void onComplete(@NonNull Task<DocumentSnapshot> task) {
          if (task.isSuccessful()) {

            OrdersCollection currOrders = task.getResult().toObject(OrdersCollection.class);
            //create an order
            int orderId = currOrders.getOrders().size();
            Order newOrder = new Order(User.currentUID, orderItems, String.valueOf(orderId));
            //add order to user's order collection
            currOrders.addOrder(newOrder);

            //update on firebase
            ordersRef.update("orders", currOrders.convertToMap());

            listener.onSuccess();

          } else {
            listener.onFailure();
          }
        }
      });
    }
  }

  public void addOrderDocForUser(final CreateNewOrderDocumentCompletionListener listner){
    Map<String, Object> orders = new HashMap<>();
    orders.put("orders", new ArrayList<Order>());

    mFirestore.collection("Orders").add(orders).addOnSuccessListener(new OnSuccessListener<DocumentReference>() {
      @Override
      public void onSuccess(DocumentReference documentReference) {
        Log.d(TAG, "new order document created with id: " + documentReference.getId());
        //set user order ref
        User.currentUser.setOrdersRef(documentReference.getId());
        //update ordersRefId in firebase
        DocumentReference user = mFirestore.collection("Users").document(User.currentUID);
        user.update("ordersRef", documentReference.getId());

        listner.onSuccess();
      }}).addOnFailureListener(new OnFailureListener() {
      @Override
      public void onFailure(@NonNull Exception e) {
        listner.onFailure();
      }
    });
  }

  public void getReviews(final SingleMenuItem menuItem, final GetMenuItemReviewsCompletionListener listener)
  {

    final DocumentReference reviewsRef =
            mFirestore.collection("Reviews").document(menuItem.getReviewsRefId());

    reviewsRef.get().addOnCompleteListener(new OnCompleteListener<DocumentSnapshot>() {
      @Override
      public void onComplete(@NonNull Task<DocumentSnapshot> task) {

        if (task.isSuccessful()) {

          ReviewsCollection currReviews = task.getResult().toObject(ReviewsCollection.class);

          if(currReviews != null)
            listener.onSuccess(currReviews.getReviews());
          else
            listener.onFailure();

        } else {

          listener.onFailure();
        }

      }
    });

  }


}
