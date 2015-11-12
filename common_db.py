import mysql.connector

def db_follow (id, screen_name):
    try:
        cnx = mysql.connector.connect(user='rational', password='Gr33nhat',
                                      host='localhost',
                                      database='twitabout')
        cur = cnx.cursor(buffered=True)
        sql = "INSERT INTO follow_log (id, screen_name) VALUES (%s, %s)"
        cur.execute(sql, (id, screen_name))
        cnx.commit()
        cnx.close()

    except mysql.connector.Error as err:
        print(err)
        cnx.close()


